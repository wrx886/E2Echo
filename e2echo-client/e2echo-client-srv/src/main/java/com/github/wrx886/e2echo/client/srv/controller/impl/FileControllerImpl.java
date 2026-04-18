package com.github.wrx886.e2echo.client.srv.controller.impl;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.srv.FileController;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.model.vo.FileVo;
import com.github.wrx886.e2echo.client.srv.service.FileService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class FileControllerImpl implements FileController {

    private final FileService fileService;

    /**
     * 发送文件
     *
     * @param toPublicKeyHex 接收者公钥
     * @param filePath       文件路径
     * @param type           文件类型
     */
    @Override
    public void sendOneFile(String toPublicKeyHex, String filePath, MessageType type) {
        fileService.sendOneFile(toPublicKeyHex, filePath, type);
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
        fileService.sendGroupFile(groupUuid, filePath, type);
    }

    /**
     * 下载文件，如果文件已经下载，则不会执行任何操作
     *
     * @param fileVo 文件信息
     */
    @Override
    public void downloadFile(FileVo fileVo) {
        fileService.downloadFile(fileVo);
    }

}
