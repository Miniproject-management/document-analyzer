package com.mini3.backend.global.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
// 프론트엔드(S3 등 다른 도메인)에서 오는 API 요청을 허용하기 위한 CORS 설정 (매우 중요)
@CrossOrigin(origins = "*")
public class HealthCheckController {

    @GetMapping("/health")
    public String healthcheck() {
        return "ok";
    }
}
