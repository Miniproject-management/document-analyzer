package com.mini3.backend.domain.ats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * HR 사내 대시보드: 지원자 한 줄 + 최신 이력서·분석 요약.
 * 지원자(Applicant) ↔ 최신 {@code Resume}(S3 키) ↔ 최신 {@code ResumeAnalysis} 매칭은 서비스에서 조합한다.
 */
public final class AtsHrDashboardDto {

    private AtsHrDashboardDto() {}

    @Getter
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantRow {
        private Long applicantId;
        private String name;
        private String email;
        private String phone;
        private LocalDateTime submittedAt;

        private Long resumeId;
        /** 제출 이력서(S3) 존재 여부 */
        private boolean resumeAttached;

        /** 최신 분석 상태 문자열. 분석 이력 없으면 null */
        private String analysisStatus;
        /** 최신 분석 총점. 없으면 null */
        private Integer overallScore;
        private LocalDateTime analyzedAt;
        /** 요약 일부 (목록용, 없으면 null) */
        private String summaryPreview;
    }
}
