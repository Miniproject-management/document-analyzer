package com.mini3.backend.domain.ats.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * management-back 은 수정하지 않으므로, 잘못된 JSON 요청은 document-analyzer 에서만 400 으로 안내한다.
 */
@Slf4j
@Order(0)
@RestControllerAdvice
public class AtsHttpMessageExceptionAdvice {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 파싱 실패: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error",
                        "JSON 형식이 올바르지 않습니다. Body는 raw → JSON 으로 두고 "
                                + "예: {\"jobDescription\":\"직무 설명\"} 처럼 중괄호로 감싸 주세요. 직무 없이 분석하려면 {} 또는 본문 생략."
                ));
    }
}
