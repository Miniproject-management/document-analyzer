package com.mini3.backend.domain.ats.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * ATS(이력서 업로드·분석) 전용 S3 클라이언트. 팀 정책상 S3·문서 파이프라인은 ats 패키지 경계 안에서 다룬다.
 */
@Configuration
public class AtsS3ClientConfig {

    @Bean
    public S3Client s3Client(@Value("${cloud.aws.region:ap-northeast-2}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
