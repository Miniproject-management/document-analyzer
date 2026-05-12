package com.mini3.backend.domain.ats.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini3.backend.domain.ats.dto.AtsAnalysisDto;
import com.mini3.backend.domain.ats.entity.Applicant;
import com.mini3.backend.domain.ats.entity.Resume;
import com.mini3.backend.domain.ats.entity.ResumeAnalysis;
import com.mini3.backend.domain.ats.enums.AnalysisStatus;
import com.mini3.backend.domain.ats.repository.ApplicantRepository;
import com.mini3.backend.domain.ats.repository.ResumeAnalysisRepository;
import com.mini3.backend.domain.ats.repository.ResumeRepository;
import com.mini3.backend.domain.ats.storage.AtsS3StorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * DB의 지원자·이력서(행)와 S3 객체 키가 연결된 경우에만 분석한다.
 * PDF 본문 추출 후 Gemini로 점수·요약·JSON 결과를 저장한다.
 * 문서 분석 파이프라인은 {@code domain.ats.analysis}에 두며, {@code domain/ats}의 컨테이너 분리 전략과 맞춘다.
 */
@Service
@RequiredArgsConstructor
public class AtsResumeAnalysisService {

    private static final int FAILURE_MSG_MAX = 1900;

    private final ApplicantRepository applicantRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final AtsS3StorageService atsS3StorageService;
    private final AtsPdfTextExtractor atsPdfTextExtractor;
    private final AtsGeminiResumeAnalysisClient atsGeminiResumeAnalysisClient;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.model:gemini-2.5-flash}")
    private String geminiModelId;

    @Transactional
    public AtsAnalysisDto.AnalyzeResult analyzeApplicantResume(Long applicantId, String jobCriteriaSnapshot) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("지원자를 찾을 수 없습니다."));
        Resume resume = resumeRepository.findFirstByApplicant_ApplicantIdOrderByCreatedAtDesc(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("제출된 이력서가 없습니다."));

        if (!resume.getApplicant().getApplicantId().equals(applicant.getApplicantId())) {
            return failResult(
                    resume,
                    "이력서 레코드가 해당 지원자와 연결되어 있지 않습니다. 데이터 정합성을 확인하세요.",
                    jobCriteriaSnapshot);
        }

        if (resume.getS3ObjectKey() == null || resume.getS3ObjectKey().isBlank()) {
            return failResult(resume, "S3 객체 키가 없습니다. 제출 파이프라인을 확인하세요.", jobCriteriaSnapshot);
        }

        try {
            byte[] bytes = atsS3StorageService.getObjectBytes(resume.getS3ObjectKey());
            if (!isAcceptedPdf(bytes, resume.getOriginalFileName())) {
                return failResult(resume, "PDF 파일만 분석할 수 있습니다. (시그니처 또는 확장자 확인)", jobCriteriaSnapshot);
            }

            String plain = atsPdfTextExtractor.extractText(bytes).trim();
            if (plain.isEmpty()) {
                return failResult(resume, "PDF에서 추출한 텍스트가 비어 있습니다. 스캔 PDF이거나 인코딩 문제일 수 있습니다.", jobCriteriaSnapshot);
            }

            JsonNode node = atsGeminiResumeAnalysisClient.analyze(applicant, resume, plain, jobCriteriaSnapshot);
            ResumeAnalysis saved = persistSuccess(resume, node, jobCriteriaSnapshot);

            return AtsAnalysisDto.AnalyzeResult.builder()
                    .message("분석이 완료되었습니다.")
                    .analysis(AtsAnalysisDto.Analysis.from(saved))
                    .build();
        } catch (Exception e) {
            ResumeAnalysis saved = persistFailure(resume, truncate(e.getMessage()), jobCriteriaSnapshot);
            return AtsAnalysisDto.AnalyzeResult.builder()
                    .message("분석에 실패했습니다.")
                    .analysis(AtsAnalysisDto.Analysis.from(saved))
                    .build();
        }
    }

    private boolean isAcceptedPdf(byte[] bytes, String originalFileName) {
        if (atsPdfTextExtractor.isLikelyPdf(bytes)) {
            return true;
        }
        if (originalFileName != null && originalFileName.toLowerCase().endsWith(".pdf")) {
            return true;
        }
        return false;
    }

    private ResumeAnalysis persistSuccess(Resume resume, JsonNode node, String jobCriteriaSnapshot)
            throws JsonProcessingException {
        int score = clampScore(node.path("overallScore").asInt(0));
        String summary = node.path("summary").asText("").trim();
        String resultJson = objectMapper.writeValueAsString(node);
        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .resume(resume)
                .status(AnalysisStatus.COMPLETED)
                .model(geminiModelId)
                .summary(summary.isEmpty() ? "(요약 없음)" : summary)
                .resultJson(resultJson)
                .overallScore(score)
                .jobCriteriaSnapshot(jobCriteriaSnapshot)
                .analyzedAt(LocalDateTime.now())
                .build();
        return resumeAnalysisRepository.save(analysis);
    }

    private ResumeAnalysis persistFailure(Resume resume, String message, String jobCriteriaSnapshot) {
        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .resume(resume)
                .status(AnalysisStatus.FAILED)
                .model(geminiModelId)
                .failureMessage(message)
                .jobCriteriaSnapshot(jobCriteriaSnapshot)
                .analyzedAt(LocalDateTime.now())
                .build();
        return resumeAnalysisRepository.save(analysis);
    }

    private AtsAnalysisDto.AnalyzeResult failResult(Resume resume, String message, String jobCriteriaSnapshot) {
        ResumeAnalysis saved = persistFailure(resume, message, jobCriteriaSnapshot);
        return AtsAnalysisDto.AnalyzeResult.builder()
                .message(message)
                .analysis(AtsAnalysisDto.Analysis.from(saved))
                .build();
    }

    private static int clampScore(int s) {
        return Math.max(0, Math.min(100, s));
    }

    private static String truncate(String m) {
        if (m == null) {
            return null;
        }
        return m.length() <= FAILURE_MSG_MAX ? m : m.substring(0, FAILURE_MSG_MAX);
    }
}
