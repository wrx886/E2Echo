package com.github.wrx886.e2echo.client.srv.service;

import java.util.List;

public interface GroupManageService {

    /**
     * 创建群聊
     * 
     * @return 群聊UUID
     */
    void create();

    /**
     * 刷新群密钥
     * 
     * @param groupUuid 群聊UUID
     */
    void reflushKey(String groupUuid);

    /**
     * 重新分发原有群密钥
     * 
     * @param groupUuid
     */
    void redistributeKey(String groupUuid);

    /**
     * 发送群密钥
     * 
     * @param groupUuid    群聊UUID
     * @param publicKeyHex 接收者公钥
     */
    void sendKey(String groupUuid, String publicKeyHex);

    /**
     * 列出所有群聊（用户作为群主）
     * 
     * @return 群聊UUID列表
     */
    List<String> listGroup();

}
