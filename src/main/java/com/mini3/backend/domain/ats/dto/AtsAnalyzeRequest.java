package com.mini3.backend.domain.ats.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HR 이력서 분석 요청. 직무·평가 기준을 넣으면 해당 기준 대비 적합도를 반영한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AtsAnalyzeRequest {

    /** HR이 입력한 채용 직무·요구역량·평가 관점 (비우면 기존과 같이 일반 관점 분석) */
    @Size(max = 3000, message = "직무 설명은 3000자 이하로 입력하세요.")
    private String jobDescription;
}
