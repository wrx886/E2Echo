package com.github.wrx886.e2echo.server.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 文件上传
     * 
     * @param file 文件
     * @return 文件ID
     */
    String upload(MultipartFile file);

    /**
     * 文件下载
     * 
     * @param fileId 文件ID
     * @return 文件
     */
    ResponseEntity<InputStreamResource> download(String fileId);

}
