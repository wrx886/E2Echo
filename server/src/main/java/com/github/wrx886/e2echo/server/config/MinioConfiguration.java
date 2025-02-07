package com.github.wrx886.e2echo.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import lombok.Getter;

@Getter
@Configuration
public class MinioConfiguration {
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient(
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
