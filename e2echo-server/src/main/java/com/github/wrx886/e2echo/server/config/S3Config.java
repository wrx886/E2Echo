package com.github.wrx886.e2echo.server.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * S3 兼容对象存储配置。
 *
 * <p>按配置创建对象存储客户端。存储服务地址、凭据与区域全部来自配置，因此本地自建（如 RustFS、
 * MinIO）与云端对象存储可以共用同一套代码，只改配置即可切换。</p>
 */
@Configuration
public class S3Config {

    /**
     * 对象存储服务地址。
     */
    @Value("${s3.endpoint}")
    private String endpoint;

    /**
     * 访问密钥。
     */
    @Value("${s3.access-key}")
    private String accessKey;

    /**
     * 访问密钥对应的私钥。
     */
    @Value("${s3.secret-key}")
    private String secretKey;

    /**
     * 区域，S3 兼容存储通常使用 us-east-1。
     */
    @Value("${s3.region}")
    private String region;

    /**
     * 默认桶名。
     */
    @Getter
    @Value("${s3.bucket-name}")
    private String bucketName;

    /**
     * 创建对象存储客户端。
     *
     * <p>开启路径样式访问：S3 兼容存储大多不支持按桶名解析的域名，不开启会连不上。</p>
     *
     * @return 对象存储客户端
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)  // 关键：为 S3 兼容存储开启路径样式
                .build();
    }

}
