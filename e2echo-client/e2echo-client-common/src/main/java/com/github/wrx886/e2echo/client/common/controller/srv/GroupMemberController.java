package com.github.wrx886.e2echo.client.common.controller.srv;

import java.util.List;

public interface GroupMemberController {

    /**
     * 获取群成员
     * 
     * @param groupUuid 群聊UUID
     * @return 群成员的公钥列表
     */
    List<String> listMember(String groupUuid);

    /**
     * 添加群成员
     * 
     * @param groupUuid    群聊UUID
     * @param publicKeyHex 群成员的公钥
     */
    void addMember(String groupUuid, String publicKeyHex);

    /**
     * 移除群成员
     * 
     * @param groupUuid    群聊UUID
     * @param publicKeyHex 群成员的公钥
     */
    void removeMember(String groupUuid, String publicKeyHex);

}
