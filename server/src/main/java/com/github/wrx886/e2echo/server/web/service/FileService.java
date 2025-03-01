package com.github.wrx886.e2echo.server.web.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.wrx886.e2echo.server.config.MinioConfiguration;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

@Service
public class FileService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfiguration minioConfiguration;

    // 上传文件到 minio 并返回文件的 url
    public String upload(MultipartFile file) throws Exception {
        // 判断桶是否存在
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioConfiguration.getBucketName())
                .build());

        if (!bucketExists) {
            throw new RuntimeException("minio bucket not exist");
        }

        // 生成文件名
        String filename = generateFileName(file);

        // 上传文件
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioConfiguration.getBucketName())
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(filename)
                .contentType(file.getContentType())
                .build());

        // 返回 url
        return String.join("/", minioConfiguration.getEndpoint(), minioConfiguration.getBucketName(), filename);
    }

    // 文件名生成器
    private static String generateFileName(MultipartFile file) {
        // 获取文件扩展名
        String extension = "";
        int i = file.getOriginalFilename().lastIndexOf('.');
        if (i >= 0) {
            extension = file.getOriginalFilename().substring(i);
        }

        // 当前日期（文件夹） + UUID（文件名）
        return new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/"
                + UUID.randomUUID() + extension;
    }

}
