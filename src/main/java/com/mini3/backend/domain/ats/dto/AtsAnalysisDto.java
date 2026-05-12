package com.mini3.backend.domain.ats.dto;

import com.mini3.backend.domain.ats.entity.ResumeAnalysis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// AI 분석 결과·분석 실행 응답.
public final class AtsAnalysisDto {

    private AtsAnalysisDto() {}

    @Getter
    @Builder // 빌더 패턴 사용
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Analysis {
        private Long analysisId;
        private String status; // 분석 상태
        private String model; // 모델 이름
        private String summary; // 요약
        private String resultJson; // 결과 JSON
        private Integer overallScore; // 총점
        private String failureMessage; // 실패 메시지
        /** 분석 요청 시 HR이 넣은 직무·평가 기준 (저장된 스냅샷) */
        private String jobCriteriaSnapshot;
        private LocalDateTime analyzedAt; // 분석 일시

        public static Analysis from(ResumeAnalysis entity) { // 엔티티를 DTO로 변환
            if (entity == null) {
                return null;
            }
            return Analysis.builder()
                    .analysisId(entity.getAnalysisId())
                    .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                    .model(entity.getModel())
                    .summary(entity.getSummary())
                    .resultJson(entity.getResultJson())
                    .overallScore(entity.getOverallScore())
                    .failureMessage(entity.getFailureMessage())
                    .jobCriteriaSnapshot(entity.getJobCriteriaSnapshot())
                    .analyzedAt(entity.getAnalyzedAt())
                    .build();
        }
    }

    // HR 이 분석하기 눌렀을 때 API 응답용으로 쓰려는 DTO
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyzeResult {
        private String message; // 분석 완료 메시지
        private Analysis analysis;  // 분석 결과 객체
    }
}
