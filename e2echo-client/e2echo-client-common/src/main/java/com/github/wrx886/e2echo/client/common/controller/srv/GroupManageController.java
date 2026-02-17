package com.github.wrx886.e2echo.client.common.controller.srv;

import java.util.List;

public interface GroupManageController {

    /**
     * 创建群聊
     * 
     * @return 群聊UUID
     */
    void create();

    /**
     * 列出所有群聊（用户作为群主）
     * 
     * @return 群聊UUID列表
     */
    List<String> listGroup();

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
