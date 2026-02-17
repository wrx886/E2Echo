package com.github.wrx886.e2echo.client.srv.controller.impl;

import java.util.List;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.srv.GroupManageController;
import com.github.wrx886.e2echo.client.srv.service.GroupManageService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class GroupManageControllerImpl implements GroupManageController {

    private final GroupManageService groupManageService;

    /**
     * 创建群聊
     * 
     * @return 群聊UUID
     */
    @Override
    public void create() {
        groupManageService.create();
    }

    /**
     * 刷新群密钥
     * 
     * @param groupUuid 群聊UUID
     */
    @Override
    public void reflushKey(String groupUuid) {
        groupManageService.reflushKey(groupUuid);
    }

    /**
     * 重新分发原有群密钥
     * 
     * @param groupUuid
     */
    @Override
    public void redistributeKey(String groupUuid) {
        groupManageService.redistributeKey(groupUuid);
    }

    /**
     * 列出所有群聊（用户作为群主）
     * 
     * @return 群聊UUID列表
     */
    @Override
    public List<String> listGroup() {
        return groupManageService.listGroup();
    }

}
