package com.github.wrx886.e2echo.client.srv.service;

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

}
