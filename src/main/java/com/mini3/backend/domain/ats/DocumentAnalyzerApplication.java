package com.mini3.backend.domain.ats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.mini3.backend.domain.ats", "com.mini3.backend.global"})
public class DocumentAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentAnalyzerApplication.class, args);
    }
}