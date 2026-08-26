package com.samsam55.trip.upload.config;

import com.samsam55.trip.upload.service.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3Presigner Bean 등록. 자격증명은 기본 Provider Chain을 쓴다
 * (로컬은 .env의 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY, 배포 환경은 IAM 역할).
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final UploadProperties uploadProperties;

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(uploadProperties.region()))
                .build();
    }
}
