package com.github.wrx886.e2echo.client.srv.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKeyShared;

public interface GroupKeySharedService extends IService<GroupKeyShared> {

    /**
     * 添加
     * 
     * @param groupUuid 群聊UUID
     * @param from      分享者公钥
     * @param to        接收者公钥
     */
    void add(String groupUuid, String from, String to);

    /**
     * 删除
     * 
     * @param groupUuid 群聊UUID
     * @param from      分享者公钥
     * @param to        接收者公钥
     */
    void remove(String groupUuid, String from, String to);

    /**
     * 获取群密钥共享列表
     * 
     * @return 群密钥共享列表
     */
    List<GroupKeyShared> listGroupKeyShared();

    /**
     * 群密钥更新监听
     * 
     * @param groupUuid 群聊UUID
     */
    void groupKeyUpdateListener(String groupUuid);

    /**
     * 规则是否存在
     * 
     * @param groupUuid 群聊UUID
     * @param from      发送者公钥
     * @param to        接收者公钥
     * @return true：存在，false：不存在
     */
    boolean contains(String groupUuid, String from, String to);

    /**
     * 重新分发一次所有规则（仅作为共享者）
     */
    void resendAll();

}
