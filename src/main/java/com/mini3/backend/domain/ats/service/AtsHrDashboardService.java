package com.mini3.backend.domain.ats.service;

import com.mini3.backend.domain.ats.dto.AtsHrDashboardDto;
import com.mini3.backend.domain.ats.entity.Applicant;
import com.mini3.backend.domain.ats.entity.Resume;
import com.mini3.backend.domain.ats.entity.ResumeAnalysis;
import com.mini3.backend.domain.ats.repository.ApplicantRepository;
import com.mini3.backend.domain.ats.repository.ResumeAnalysisRepository;
import com.mini3.backend.domain.ats.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HR 대시보드: 지원자 목록과 최신 이력서·분석 요약을 한 행으로 묶어 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtsHrDashboardService {

    private static final int SUMMARY_PREVIEW_MAX = 160;

    private final ApplicantRepository applicantRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    public List<AtsHrDashboardDto.ApplicantRow> listApplicantRows() {
        return applicantRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toRow)
                .toList();
    }

    private AtsHrDashboardDto.ApplicantRow toRow(Applicant applicant) {
        var row = AtsHrDashboardDto.ApplicantRow.builder()
                .applicantId(applicant.getApplicantId())
                .name(applicant.getName())
                .email(applicant.getEmail())
                .phone(applicant.getPhone())
                .submittedAt(applicant.getCreatedAt())
                .resumeAttached(false)
                .build();

        return resumeRepository.findFirstByApplicant_ApplicantIdOrderByCreatedAtDesc(applicant.getApplicantId())
                .map(resume -> enrichWithResume(row, resume))
                .orElse(row);
    }

    private AtsHrDashboardDto.ApplicantRow enrichWithResume(AtsHrDashboardDto.ApplicantRow base, Resume resume) {
        var builder = base.toBuilder()
                .resumeId(resume.getResumeId())
                .resumeAttached(resume.getS3ObjectKey() != null && !resume.getS3ObjectKey().isBlank());

        resumeAnalysisRepository.findFirstByResume_ResumeIdOrderByAnalyzedAtDesc(resume.getResumeId())
                .ifPresent(analysis -> applyLatestAnalysis(builder, analysis));

        return builder.build();
    }

    private void applyLatestAnalysis(AtsHrDashboardDto.ApplicantRow.ApplicantRowBuilder builder, ResumeAnalysis analysis) {
        builder.analysisStatus(analysis.getStatus() != null ? analysis.getStatus().name() : null)
                .overallScore(analysis.getOverallScore())
                .analyzedAt(analysis.getAnalyzedAt())
                .summaryPreview(truncate(analysis.getSummary()));
    }

    private static String truncate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        if (t.length() <= SUMMARY_PREVIEW_MAX) {
            return t;
        }
        return t.substring(0, SUMMARY_PREVIEW_MAX) + "…";
    }
}
