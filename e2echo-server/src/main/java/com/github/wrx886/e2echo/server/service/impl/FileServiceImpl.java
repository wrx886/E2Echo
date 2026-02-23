package com.github.wrx886.e2echo.server.service.impl;

import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.wrx886.e2echo.server.config.MinIOConfig;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.service.FileService;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;

    /**
     * 文件上传
     * 
     * @param file 文件
     * @return 文件ID
     */
    @Override
    public String upload(MultipartFile file) {
        try {
            // 生成文件名
            String fileName = generateFileName(file);

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minIOConfig.getBucketName())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .object(fileName)
                    .contentType(file.getContentType())
                    .build());

            // 返回文件id
            return fileName;

        } catch (E2EchoException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 文件名生成器
    private static String generateFileName(MultipartFile file) {
        // 获取文件扩展名
        String extension = "";
        int i = file.getOriginalFilename().lastIndexOf('.');
        if (i >= 0) {
            extension = file.getOriginalFilename().substring(i);
        }

        // 时间戳 - UUID（文件名）
        return Long.toString(System.currentTimeMillis()) + "-" + UUID.randomUUID() + extension;
    }

    /**
     * 文件下载
     * 
     * @param fileId 文件ID
     * @return 文件
     */
    @Override
    public ResponseEntity<Resource> download(String fileId) {
        try {
            // 获取文件输入流
            GetObjectResponse stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minIOConfig.getBucketName())
                    .object(fileId)
                    .build());

            // 返回
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + stream.object() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
