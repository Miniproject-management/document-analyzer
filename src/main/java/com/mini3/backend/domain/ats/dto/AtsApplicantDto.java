package com.mini3.backend.domain.ats.dto;

import com.mini3.backend.domain.ats.entity.Applicant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 지원자 목록 상세 API
public final class AtsApplicantDto {

    private AtsApplicantDto() {}

    // 지원자 목록 한 줄
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private Long applicantId;
        private String name; // 지원자 이름
        private String email; // 지원자 이메일
        private String phone; // 지원자 전화번호
        private LocalDateTime submittedAt; // 지원자 제출 일시

        public static ListItem from(Applicant applicant) { // 엔티티를 DTO로 변환
            return ListItem.builder()
                    .applicantId(applicant.getApplicantId())
                    .name(applicant.getName())
                    .email(applicant.getEmail())
                    .phone(applicant.getPhone())
                    .submittedAt(applicant.getCreatedAt())
                    .build();
        }
    }

    // 한 명 클릭 시 상세 정보
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private Long applicantId;
        private String name;
        private String email;
        private String phone;
        private LocalDateTime submittedAt;

        private Long resumeId;
        private String resumeTitle;
        private String source;
        private String originalFileName;
        private String storageKey;

        private AtsAnalysisDto.Analysis analysis;
    }
}
