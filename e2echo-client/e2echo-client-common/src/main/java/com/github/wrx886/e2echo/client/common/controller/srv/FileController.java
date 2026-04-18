package com.github.wrx886.e2echo.client.common.controller.srv;

import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.model.vo.FileVo;

public interface FileController {

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

    /**
     * 下载文件，如果文件已经下载，则不会执行任何操作
     *
     * @param fileVo 文件信息
     */
    void downloadFile(FileVo fileVo);

}
