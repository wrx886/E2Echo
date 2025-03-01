package com.github.wrx886.e2echo.client.service;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.api.FileApi;
import com.github.wrx886.e2echo.client.mapper.FileMapper;
import com.github.wrx886.e2echo.client.model.entity.File;
import com.github.wrx886.e2echo.client.store.LoginUserStore;
import com.github.wrx886.e2echo.client.util.AesFileUtil;
import com.github.wrx886.e2echo.client.util.FileHashUtil;

@Service
public class FileService extends ServiceImpl<FileMapper, File> {

    @Autowired
    private FileApi fileApi;

    @Autowired
    private LoginUserStore loginUserStore;

    // 上传文件到服务端
    public File upload(String filePath) {

        // 生成密钥
        String aesKey = AesFileUtil.generateKeyAsHex();

        // 使用密钥加密文件
        String outputPath = "./temp/" + UUID.randomUUID().toString() + ".upload";
        AesFileUtil.encrypt(filePath, outputPath, aesKey);

        // 上传文件
        String url = fileApi.upload(loginUserStore.getBaseUrl(), loginUserStore.getAccessToken(), outputPath);

        File file = new File();
        file.setAesKey(aesKey);
        file.setFileName(new java.io.File(filePath).getName());
        file.setOwnerId(loginUserStore.getId());
        file.setPath(filePath);
        file.setSha256(FileHashUtil.calculateFileHash(filePath));
        file.setUrl(url);
        return file;
    }

    // 下载文件到本地（保存到数据库后返回）
    public File download(File sendFile) {
        try {
            // 判断文件是否存在，存在则返回，避免重复文件
            File file = getOne(new LambdaQueryWrapper<File>()
                    .eq(File::getOwnerId, loginUserStore.getId())
                    .eq(File::getUrl, sendFile.getUrl()));
            if (file != null) {
                return file;
            }

            // 本地文件路径
            String destinationPath = "./temp/" + UUID.randomUUID().toString() + ".download";

            URL url = new URL(sendFile.getUrl());
            // 打开连接并获取输入流
            try (InputStream in = url.openStream()) {
                // 定义目标路径
                Path destination = Paths.get(destinationPath);
                // 确保父目录存在
                Files.createDirectories(destination.getParent());
                // 将输入流中的数据复制到目标路径
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            // 解密文件
            String outputPath = "./download/" + UUID.randomUUID().toString() + "/" + sendFile.getFileName();
            AesFileUtil.decrypt(destinationPath, outputPath, sendFile.getAesKey());

            // 比较哈希值
            if (!FileHashUtil.calculateFileHash(outputPath).equals(sendFile.getSha256())) {
                throw new RuntimeException("文件哈希值错误");
            }

            // 构建返回值
            file = new File();
            file.setAesKey(sendFile.getAesKey());
            file.setFileName(sendFile.getFileName());
            file.setOwnerId(loginUserStore.getId());
            file.setPath(outputPath);
            file.setSha256(sendFile.getSha256());
            file.setUrl(sendFile.getUrl());
            save(file);
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
