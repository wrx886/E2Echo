package com.github.wrx886.e2echo.client.test.srv.feign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import com.github.wrx886.e2echo.client.ecc.store.EccKeyStore;
import com.github.wrx886.e2echo.client.srv.feign.FileFeign;
import com.github.wrx886.e2echo.client.srv.result.Result;
import com.github.wrx886.e2echo.client.srv.store.WebUrlStore;
import com.github.wrx886.e2echo.client.starter.Main.Starter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = { Starter.class })
@ActiveProfiles("test")
public class FileFeignTest {

    @Autowired
    private FileFeign fileFeign;

    @Autowired
    private WebUrlStore webUrlStore;

    @Autowired
    private EccKeyStore eccKeyStore;

    @BeforeEach
    public void init() {
        eccKeyStore.set(
                "04d18e146436495497953b42d4fdc2bc3134b4c63214c091c7b9bd8d8d0135f4d6709795a07206f61e530e67fc4f6e7b54daefb8eb276d46cb35f84d8e05f90dd5",
                "6869bee95e46cc8b28cd9c167285ba68a6143b63f1ba371331085c9eae355bba");
        webUrlStore.setWebUrl("http://localhost:8080");
    }

    @Test
    public void uploadDownloadTest() throws Exception {
        // 上传文件
        File file = new File("D:\\Users\\01\\Desktop\\毕设\\##毕设表格（2023.12更新）\\附件1 毕业设计任务书.doc");

        MultipartFile multipartFile;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            multipartFile = new MockMultipartFile(
                    "file",
                    file.getName(),
                    Files.probeContentType(file.toPath()),
                    inputStream);
        }

        Result<String> uploadResult = fileFeign.upload(multipartFile);
        log.info("fileId: {}", uploadResult.getData());

        // 下载文件
        try (FileOutputStream outputStream = new FileOutputStream(
                "D:\\Users\\01\\Desktop\\" + uploadResult.getData())) {
            ResponseEntity<Resource> downloadEntity = fileFeign.download(uploadResult.getData());
            downloadEntity.getBody().getInputStream().transferTo(outputStream);
        }
    }

}
