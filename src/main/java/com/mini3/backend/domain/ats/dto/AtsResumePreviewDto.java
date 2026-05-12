package com.mini3.backend.domain.ats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** S3 이력서 PDF 브라우저 미리보기용 단기 URL */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtsResumePreviewDto {

    private String url;
    /** 초 단위 (프론트 타이머·표시용) */
    private int expiresInSeconds;
}
