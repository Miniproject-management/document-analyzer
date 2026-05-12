package com.mini3.backend.domain.ats.service;

import com.mini3.backend.domain.ats.dto.AtsAnalysisDto;
import com.mini3.backend.domain.ats.dto.AtsApplicantDto;
import com.mini3.backend.domain.ats.dto.AtsHrDashboardDto;
import com.mini3.backend.domain.ats.dto.AtsSubmitDto;
import com.mini3.backend.domain.ats.entity.Applicant;
import com.mini3.backend.domain.ats.entity.Resume;
import com.mini3.backend.domain.ats.enums.ResumeSource;
import com.mini3.backend.domain.ats.repository.ApplicantRepository;
import com.mini3.backend.domain.ats.repository.ResumeAnalysisRepository;
import com.mini3.backend.domain.ats.repository.ResumeRepository;
import com.mini3.backend.domain.ats.analysis.AtsResumeAnalysisService;
import com.mini3.backend.domain.ats.storage.AtsS3StorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtsService {

    private final ApplicantRepository applicantRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final AtsS3StorageService atsS3StorageService;
    private final AtsHrDashboardService atsHrDashboardService;
    private final AtsResumeAnalysisService atsResumeAnalysisService;

    /**
     * 공개 지원: 인적사항 + 파일 1건 저장. 원본 파일은 S3에 업로드한다.
     */
    @Transactional
    public AtsSubmitDto.SubmitResponse submitApplication(AtsSubmitDto.SubmitRequest request, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("첨부 파일이 필요합니다.");
        }

        Applicant applicant = Applicant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();
        applicantRepository.save(applicant);

        String s3Key;
        try {
            s3Key = atsS3StorageService.uploadApplicantResume(file, applicant.getApplicantId());
        } catch (IOException | SdkException e) {
            throw new IllegalStateException("파일을 S3에 업로드하지 못했습니다.", e);
        }

        Resume resume = Resume.builder()
                .applicant(applicant)
                .title(null)
                .content("")
                .source(ResumeSource.UPLOAD)
                .originalFileName(file.getOriginalFilename())
                .s3ObjectKey(s3Key)
                .build();
        resumeRepository.save(resume);

        return AtsSubmitDto.SubmitResponse.builder()
                .applicantId(applicant.getApplicantId())
                .resumeId(resume.getResumeId())
                .message("제출이 완료되었습니다.")
                .build();
    }

    public List<AtsApplicantDto.ListItem> getApplicants() {
        return applicantRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AtsApplicantDto.ListItem::from)
                .toList();
    }

    /** HR 대시보드 테이블: 지원자 + 최신 이력서·분석 요약 한 행 */
    public List<AtsHrDashboardDto.ApplicantRow> getHrDashboardApplicantRows() {
        return atsHrDashboardService.listApplicantRows();
    }

    public AtsApplicantDto.Detail getApplicantDetail(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("지원자를 찾을 수 없습니다."));

        AtsApplicantDto.Detail.DetailBuilder builder = AtsApplicantDto.Detail.builder()
                .applicantId(applicant.getApplicantId())
                .name(applicant.getName())
                .email(applicant.getEmail())
                .phone(applicant.getPhone())
                .submittedAt(applicant.getCreatedAt());

        resumeRepository.findFirstByApplicant_ApplicantIdOrderByCreatedAtDesc(applicantId)
                .ifPresent(resume -> {
                    builder.resumeId(resume.getResumeId())
                            .resumeTitle(resume.getTitle())
                            .source(resume.getSource() != null ? resume.getSource().name() : null)
                            .originalFileName(resume.getOriginalFileName())
                            .storageKey(resume.getS3ObjectKey());

                    resumeAnalysisRepository.findFirstByResume_ResumeIdOrderByAnalyzedAtDesc(resume.getResumeId())
                            .ifPresent(analysis -> builder.analysis(AtsAnalysisDto.Analysis.from(analysis)));
                });

        return builder.build();
    }

    /**
     * 최근 제출 이력서 기준 AI 분석. 구현은 {@link AtsResumeAnalysisService}에 위임한다.
     */
    @Transactional
    public AtsAnalysisDto.AnalyzeResult analyzeApplicantResume(Long applicantId, String jobDescription) {
        String jobCriteriaSnapshot = normalizeJobCriteria(jobDescription);
        return atsResumeAnalysisService.analyzeApplicantResume(applicantId, jobCriteriaSnapshot);
    }

    private static String normalizeJobCriteria(String jobDescription) {
        if (jobDescription == null) {
            return null;
        }
        String t = jobDescription.strip();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > 3000 ? t.substring(0, 3000) : t;
    }
}
