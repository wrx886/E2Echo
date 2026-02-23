package com.github.wrx886.e2echo.client.srv.service;

import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;

public interface FileService {

    /**
     * 发送文件
     *
     * @param toPublicKeyHex 接收者公钥
     * @param filePath       文件路径
     * @param type           文件类型
     */
    void sendOneFile(String toPublicKeyHex, String filePath, MessageType type);

    /**
     * 发送群文件
     *
     * @param groupUuid 群 UUID
     * @param filePath  文件路径
     * @param type      文件类型
     */
    void sendGroupFile(String groupUuid, String filePath, MessageType type);

}
