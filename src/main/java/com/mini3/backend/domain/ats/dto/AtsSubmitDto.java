package com.mini3.backend.domain.ats.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 공개 지원서 제출 API. */
public final class AtsSubmitDto {

    private AtsSubmitDto() {}

    @Getter
    @Setter
    public static class SubmitRequest {
        @NotBlank
        private String name;
        private String email;
        private String phone;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitResponse {
        private Long applicantId;
        private Long resumeId;
        private String message;
    }
}
