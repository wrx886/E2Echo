package com.github.wrx886.e2echo.client.srv.controller.impl;

import java.util.List;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.srv.GroupMemberController;
import com.github.wrx886.e2echo.client.srv.service.GroupMemberService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class GroupMemberControllerImpl implements GroupMemberController {

    private final GroupMemberService groupMemberService;

    /**
     * 获取群成员
     * 
     * @param groupUuid 群聊UUID
     * @return 群成员的公钥列表
     */
    @Override
    public List<String> listMember(String groupUuid) {
        return groupMemberService.listMember(groupUuid);
    }

    /**
     * 添加群成员
     * 
     * @param groupUuid    群聊UUID
     * @param publicKeyHex 群成员的公钥
     */
    @Override
    public void addMember(String groupUuid, String publicKeyHex) {
        groupMemberService.addMember(groupUuid, publicKeyHex);
    }

    /**
     * 移除群成员
     * 
     * @param groupUuid    群聊UUID
     * @param publicKeyHex 群成员的公钥
     */
    @Override
    public void removeMember(String groupUuid, String publicKeyHex) {
        groupMemberService.removeMember(groupUuid, publicKeyHex);
    }

}
