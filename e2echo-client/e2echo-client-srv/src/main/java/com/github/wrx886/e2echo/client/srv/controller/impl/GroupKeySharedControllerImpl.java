package com.github.wrx886.e2echo.client.srv.controller.impl;

import java.util.List;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.srv.GroupKeySharedController;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKeyShared;
import com.github.wrx886.e2echo.client.srv.service.GroupKeySharedService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class GroupKeySharedControllerImpl implements GroupKeySharedController {

    private final GroupKeySharedService groupKeySharedService;

    /**
     * 添加群密钥共享规则
     * 
     * @param groupUuid
     * @param from
     * @param to
     */
    @Override
    public void add(String groupUuid, String from, String to) {
        groupKeySharedService.add(groupUuid, from, to);
    }

    /**
     * 删除群密钥共享规则
     * 
     * @param groupUuid
     * @param from
     * @param to
     */
    @Override
    public void remove(String groupUuid, String from, String to) {
        groupKeySharedService.remove(groupUuid, from, to);
    }

    /**
     * 获取群密钥共享列表
     * 
     * @return 所有共享规则
     */
    @Override
    public List<GroupKeyShared> listGroupKeyShared() {
        return groupKeySharedService.listGroupKeyShared();
    }

    /**
     * 重新分发一次所有规则（仅作为共享者）
     */
    @Override
    public void resendAll() {
        groupKeySharedService.resendAll();
    }

}
