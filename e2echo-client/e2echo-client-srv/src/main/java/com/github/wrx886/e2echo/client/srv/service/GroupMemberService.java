package com.github.wrx886.e2echo.client.srv.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.wrx886.e2echo.client.common.model.entity.GroupMember;

public interface GroupMemberService extends IService<GroupMember> {

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
     * @param publicKeyHex 群成员公钥
     */
    void addMember(String groupUuid, String publicKeyHex);

    /**
     * 删除群成员
     * 
     * @param groupUuid    群聊UUID
     * @param publicKeyHex 群成员公钥
     */
    void removeMember(String groupUuid, String publicKeyHex);

}
