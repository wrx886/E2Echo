package com.github.wrx886.e2echo.client.srv.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.model.vo.FileVo;
import com.github.wrx886.e2echo.client.srv.common.MockMultipartFile;
import com.github.wrx886.e2echo.client.srv.feign.FileFeign;
import com.github.wrx886.e2echo.client.srv.result.Result;
import com.github.wrx886.e2echo.client.srv.result.ResultCodeEnum;
import com.github.wrx886.e2echo.client.srv.service.FileService;
import com.github.wrx886.e2echo.client.srv.service.MessageService;
import com.github.wrx886.e2echo.client.srv.util.AesUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileFeign fileFeign;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    /**
     * 发送文件
     *
     * @param toPublicKeyHex 接收者公钥
     * @param filePath       文件路径
     * @param type           文件类型
     */
    @Override
    public void sendOneFile(String toPublicKeyHex, String filePath, MessageType type) {
        try {
            // 上传文件
            FileVo fileVo = upload(filePath);

            // 发送消息
            String fileVoString = objectMapper.writeValueAsString(fileVo);
            messageService.sendOne(toPublicKeyHex, fileVoString, type);
        } catch (E2EchoException e) {
            throw e;
        } catch (Exception e) {
            throw new E2EchoException(e.getMessage());
        }
    }

    /**
     * 发送群文件
     *
     * @param groupUuid 群 UUID
     * @param filePath  文件路径
     * @param type      文件类型
     */
    @Override
    public void sendGroupFile(String groupUuid, String filePath, MessageType type) {
        try {
            // 上传文件
            FileVo fileVo = upload(filePath);

            // 发送消息
            String fileVoString = objectMapper.writeValueAsString(fileVo);
            messageService.sendOne(groupUuid, fileVoString, type);
        } catch (E2EchoException e) {
            throw e;
        } catch (Exception e) {
            throw new E2EchoException(e.getMessage());
        }
    }

    private FileVo upload(String filePath) {
        try {
            // 创建文件夹
            File dir = new File("./temp");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 加密文件
            File file = new File(filePath);
            String aesKey = AesUtil.generateKeyAsHex();
            String encryptFilePath = "./temp/" + UUID.randomUUID() + ".aes";
            AesUtil.encryptFile(filePath, encryptFilePath, aesKey);

            // 上传文件
            File encryptedFile = new File(encryptFilePath);
            MultipartFile multipartFile;
            try (InputStream inputStream = new FileInputStream(encryptFilePath)) {
                multipartFile = new MockMultipartFile(
                        "file",
                        encryptedFile.getName(),
                        Files.probeContentType(encryptedFile.toPath()),
                        inputStream);
            }
            Result<String> uploadResult = fileFeign.upload(multipartFile);
            if (!ResultCodeEnum.OK.getCode().equals(uploadResult.getCode())) {
                throw new E2EchoException(uploadResult.getMessage());
            }

            // 构建返回值
            FileVo fileVo = new FileVo();
            fileVo.setFileId(uploadResult.getData());
            fileVo.setAesKey(aesKey);
            fileVo.setFileName(file.getName());

            // 删除临时文件
            new File(encryptFilePath).delete();

            return fileVo;
        } catch (E2EchoException e) {
            throw e;
        } catch (Exception e) {
            throw new E2EchoException(e.getMessage());
        }
    }

}
