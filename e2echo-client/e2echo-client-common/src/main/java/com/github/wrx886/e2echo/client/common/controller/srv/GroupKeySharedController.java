package com.github.wrx886.e2echo.client.common.controller.srv;

import java.util.List;

import com.github.wrx886.e2echo.client.common.model.entity.GroupKeyShared;

public interface GroupKeySharedController {

    /**
     * 添加群密钥共享规则
     * 
     * @param groupUuid
     * @param from
     * @param to
     */
    void add(String groupUuid, String from, String to);

    /**
     * 删除群密钥共享规则
     * 
     * @param groupUuid
     * @param from
     * @param to
     */
    void remove(String groupUuid, String from, String to);

    /**
     * 获取群密钥共享列表
     * 
     * @return 所有共享规则
     */
    List<GroupKeyShared> listGroupKeyShared();

    /**
     * 重新分发一次所有规则（仅作为共享者）
     */
    void resendAll();

}
