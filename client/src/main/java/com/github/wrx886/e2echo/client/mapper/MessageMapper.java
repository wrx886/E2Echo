package com.github.wrx886.e2echo.client.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.wrx886.e2echo.client.model.entity.Message;
import com.github.wrx886.e2echo.client.model.vo.MessageVo;
import com.github.wrx886.e2echo.client.model.vo.SessionVo;

public interface MessageMapper extends BaseMapper<Message> {

    // 获取会话信息
    List<SessionVo> listSessionVos(Long ownerId);

    // 根据会话 id 获取消息
    List<MessageVo> listMessageVoBySessionId(Long sessionId);

}
