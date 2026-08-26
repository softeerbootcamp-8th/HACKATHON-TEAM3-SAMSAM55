package com.samsam55.trip.upload.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3 업로드 설정. {@code upload.*} (application.yaml, 실제 값은 .env)로 바인딩된다.
 */
@ConfigurationProperties(prefix = "upload")
public record UploadProperties(
        String bucketName,
        String region
) {
}
