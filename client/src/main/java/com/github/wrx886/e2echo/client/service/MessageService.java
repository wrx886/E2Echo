package com.github.wrx886.e2echo.client.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.api.MessageApi;
import com.github.wrx886.e2echo.client.api.MessageWebSocketApi;
import com.github.wrx886.e2echo.client.common.E2Echoxception;
import com.github.wrx886.e2echo.client.common.WebSocketResult;
import com.github.wrx886.e2echo.client.mapper.GroupUserMapper;
import com.github.wrx886.e2echo.client.mapper.MessageMapper;
import com.github.wrx886.e2echo.client.model.api.MessageApiVo;
import com.github.wrx886.e2echo.client.model.api.ReceiveMessageApiVo;
import com.github.wrx886.e2echo.client.model.entity.File;
import com.github.wrx886.e2echo.client.model.entity.Message;
import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.model.enums.MessageApiType;
import com.github.wrx886.e2echo.client.model.enums.MessageType;
import com.github.wrx886.e2echo.client.model.enums.UserType;
import com.github.wrx886.e2echo.client.model.vo.MessageVo;
import com.github.wrx886.e2echo.client.model.vo.SendMessageVo;
import com.github.wrx886.e2echo.client.model.vo.SessionVo;
import com.github.wrx886.e2echo.client.store.GuiStore;
import com.github.wrx886.e2echo.client.store.LoginUserStore;
import com.github.wrx886.e2echo.client.store.MessageStore;
import com.github.wrx886.e2echo.client.util.EccUtil;
import com.github.wrx886.e2echo.client.util.JsonUtil;

import javafx.application.Platform;

// 消息服务
@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> implements InitializingBean {

    @Autowired
    private LoginUserStore loginUserStore;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageApi messageApi;

    @Autowired
    private MessageStore messageStore;

    @Autowired
    private GuiStore guiStore;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageWebSocketApi messageWebSocketApi;

    @Autowired
    private JsonUtil jsonUtil;

    @Autowired
    private GroupUserMapper groupUserMapper;

    @Autowired
    private FileService fileService;

    // 初始化后处理
    @Override
    public void afterPropertiesSet() throws Exception {
        // 这里负责绑定 messageWebSocketApi 需要处理的方法
        messageWebSocketApi.bindCommandMethod("receive", this::receiveHandler);
        messageWebSocketApi.bindCommandMethod("autoReceive", this::autoReceiveHander);
    }

    // 发送消息
    public <E> void send(Long sessionId, E messageData, MessageType messageType) {
        try {
            // 获取会话信息
            User session = userService.getById(sessionId);
            // 获取发送者信息
            User from = userService.putPersonByPublicKey(loginUserStore.getId(), loginUserStore.getPublicKey());

            // 针对消息类型处理消息
            SendMessageVo<File> sendMessageVo = new SendMessageVo<>();
            if (MessageType.TEXT.equals(messageType)) {
                if (messageData instanceof String s) {
                    sendMessageVo.setData(s);
                } else {
                    throw new RuntimeException("消息类型错误！");
                }
            } else if (MessageType.AUDIO.equals(messageType) ||
                    MessageType.FILE.equals(messageType) ||
                    MessageType.PICTURE.equals(messageType) ||
                    MessageType.VIDEO.equals(messageType)) {
                // 对于文件类型的数据进行处理，此时传入的是文件
                if (messageData instanceof File file) {
                    sendMessageVo.setSendData(file);
                } else {
                    throw new RuntimeException("消息类型错误");
                }
            } else {
                throw new UnsupportedOperationException("数据类型未处理！");
            }

            // 构建消息实体
            sendMessageVo.setFromId(from.getId());
            sendMessageVo.setFromPublicKey(loginUserStore.getPublicKey());
            sendMessageVo.setGroupUuid(session.getGroupUuid());
            sendMessageVo.setOwnerId(loginUserStore.getId());
            sendMessageVo.setSendTime(new Date());
            sendMessageVo.setSessionId(session.getId());
            sendMessageVo.setType(messageType);
            sendMessageVo.setUuid(UUID.randomUUID().toString());

            // 序列化消息实体
            String sendData;
            try {
                sendData = objectMapper.writeValueAsString(sendMessageVo);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new E2Echoxception(null, "序列化失败！");
            }

            // 构建发送参数
            MessageApiVo messageApiVo = new MessageApiVo();
            messageApiVo.setData(EccUtil.encrypt(sendData, session.getPublicKey()));
            messageApiVo.setFromPublicKey(loginUserStore.getPublicKey());
            messageApiVo.setSendTime(sendMessageVo.getSendTime());
            messageApiVo.setSign(EccUtil.sign(sendData, loginUserStore.getPrivateKey()));
            messageApiVo.setToPublicKey(session.getPublicKey());
            if (UserType.PERSON.equals(session.getType())) {
                // 个人
                messageApiVo.setMessageType(MessageApiType.USER);
            } else if (UserType.GROUP.equals(session.getType())) {
                // 群聊
                messageApiVo.setMessageType(MessageApiType.GROUP);
            } else {
                throw new E2Echoxception(null, "消息类型错误");
            }

            // 发送消息
            messageApi.send(loginUserStore.getBaseUrl(), loginUserStore.getAccessToken(), messageApiVo);

            // 插入消息到数据库
            if (MessageApiType.USER.equals(messageApiVo.getMessageType())) {
                if (sendMessageVo.getData() == null) {
                    sendMessageVo.setData(objectMapper.writeValueAsString(sendMessageVo.getSendData()));
                }
                save(sendMessageVo);
            }
        } catch (E2Echoxception e) {
            e.printStackTrace();
            throw new E2Echoxception(e.getCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // 接收消息
    public void receive() {
        // 构建消息接收参数
        ReceiveMessageApiVo receiveMessageApiVo = new ReceiveMessageApiVo();
        receiveMessageApiVo.setStartTime(messageStore.getLastUpdateTime());
        receiveMessageApiVo.setMessageType(MessageApiType.USER);
        Date lastUpdateTime = new Date();

        // 接收消息
        HashSet<MessageApiVo> messageApiVos = messageApi.receive(loginUserStore.getBaseUrl(),
                loginUserStore.getAccessToken(),
                receiveMessageApiVo);

        // 处理消息
        for (MessageApiVo messageApiVo : messageApiVos) {
            try {
                handleMessageApiVo(messageApiVo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 更新最后更新时间
        messageStore.setLastUpdateTime(lastUpdateTime);
    }

    // 更新会话列表
    public void updateSessionVos() {
        List<SessionVo> sessionVos = messageMapper.listSessionVos(loginUserStore.getId());
        guiStore.getSessionVos().clear();
        guiStore.getSessionVos().addAll(sessionVos);
    }

    // 更新消息列表
    public void updateMessageVosBySession(SessionVo sessionVo) {
        List<MessageVo> messageVos = messageMapper.listMessageVoBySessionId(sessionVo.getSessionId());
        guiStore.getCurrentMessageVos().clear();
        guiStore.getCurrentMessageVos().addAll(messageVos);
    }

    // WebSocket 处理方法-配置或取消配置自动接收
    // 如果接收群聊消息，则也会接收用户消息
    public void registryAutoReceive(MessageApiType messageApiType) {
        // 登入
        messageWebSocketApi.login(loginUserStore.getAccessToken());

        if (messageApiType == null) {
            // 取消配置
            messageWebSocketApi.cancelAutoReceive();
        } else {
            // 配置
            messageWebSocketApi.registryAutoReceive(messageApiType);
        }
    }

    // WebSocket 处理方法-消息接收
    private void receiveHandler(WebSocketResult<?> result) {

        // 处理数据为列表
        ArrayList<?> receiveList = jsonUtil.typeCast(result.getData(), ArrayList.class);

        // 去重
        HashSet<?> receiveSet = new HashSet<>(receiveList);

        // 遍历处理
        for (Object receiveData : receiveSet) {
            try {
                // 转为对应类型
                MessageApiVo messageApiVo = jsonUtil.typeCast(receiveData, MessageApiVo.class);

                // 处理消息
                handleMessageApiVo(messageApiVo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // WebSocket 处理方法-自动接收
    private void autoReceiveHander(WebSocketResult<?> result) {
        // 转换并处理
        handleMessageApiVo(jsonUtil.typeCast(result.getData(), MessageApiVo.class));
        // 这两步要在 JavaFX 线程中执行
        Platform.runLater(() -> {
            updateSessionVos();
            updateMessageVosBySession(guiStore.getCurrentSessionVo().get());
        });
    }

    // 处理一条消息
    private void handleMessageApiVo(MessageApiVo messageApiVo) {
        try {
            // 判断是否为消息接收者
            if (!loginUserStore.getPublicKey().equals(messageApiVo.getToPublicKey())) {
                throw new E2Echoxception(null, "接收者错误");
            }

            // 解包
            SendMessageVo<?> sendMessageVo;
            try {
                // 解密数据
                String data = EccUtil.decrypt(messageApiVo.getData(), loginUserStore.getPrivateKey());
                // 验证数据签名
                assert EccUtil.verify(data, messageApiVo.getSign(), messageApiVo.getFromPublicKey());
                // 将数据转为需要的格式
                sendMessageVo = objectMapper.readValue(data, SendMessageVo.class);
            } catch (Exception e) {
                e.printStackTrace();
                throw new E2Echoxception(null, e.getMessage());
            }

            // 这里只处理用户信息
            if (MessageApiType.GROUP.equals(messageApiVo.getMessageType())) {
                // 群聊消息，直接反发给所有群成员

                // 查询群聊
                User group = userService.getOne(new LambdaQueryWrapper<User>()
                        .eq(User::getOwnerId, loginUserStore.getId())
                        .eq(User::getGroupUuid, sendMessageVo.getGroupUuid())
                        .eq(User::getPublicKey, loginUserStore.getPublicKey())
                        .eq(User::getType, UserType.GROUP));

                // 群聊不存在
                if (group == null) {
                    throw new E2Echoxception(null, "群聊不存在");
                }

                // 根据群聊 UUID 查询群成员
                List<User> members = groupUserMapper.listUserByGroupId(group.getId());
                System.out.println(members);

                // 判断发送者是否存在
                boolean exist = false;
                for (User member : members) {
                    if (member.getPublicKey().equals(sendMessageVo.getFromPublicKey())) {
                        exist = true;
                        break;
                    }
                }

                // 发送者不在群内
                if (!exist) {
                    throw new E2Echoxception(null, "发送者不在群内");
                }

                // 完成反发
                System.out.println(members);
                for (User member : members) {
                    try {
                        // 构建发送参数
                        String sendData = objectMapper.writeValueAsString(sendMessageVo);
                        MessageApiVo returnMessageApiVo = new MessageApiVo();
                        returnMessageApiVo.setData(EccUtil.encrypt(sendData, member.getPublicKey()));
                        returnMessageApiVo.setFromPublicKey(loginUserStore.getPublicKey());
                        returnMessageApiVo.setMessageType(MessageApiType.USER);
                        returnMessageApiVo.setSendTime(new Date());
                        returnMessageApiVo.setSign(EccUtil.sign(sendData, loginUserStore.getPrivateKey()));
                        returnMessageApiVo.setToPublicKey(member.getPublicKey());
                        // 发送
                        messageWebSocketApi.send(returnMessageApiVo);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new E2Echoxception(null, e.getMessage());
                    }
                }
            } else if (!MessageApiType.USER.equals(messageApiVo.getMessageType())) {
                throw new E2Echoxception(null, "消息类型错误");
            }

            // 获取会话和发送者信息
            User session, from;
            if (sendMessageVo.getGroupUuid() == null) {
                // 私聊
                session = userService.putPersonByPublicKey(loginUserStore.getId(), sendMessageVo.getFromPublicKey());
                from = session;
            } else {
                // 群聊
                session = userService.putGroupByGroupUuid(loginUserStore.getId(), sendMessageVo.getGroupUuid(),
                        messageApiVo.getFromPublicKey());
                from = userService.putPersonByPublicKey(loginUserStore.getId(), sendMessageVo.getFromPublicKey());
            }

            // 填充 Message 信息
            sendMessageVo.setData(objectMapper.writeValueAsString(sendMessageVo.getSendData()));
            sendMessageVo.setFromId(from.getId());
            sendMessageVo.setOwnerId(loginUserStore.getId());
            sendMessageVo.setSessionId(session.getId());

            // 按消息类型处理消息
            if (MessageType.TEXT.equals(sendMessageVo.getType())) {
                // 文本消息，直接插入即可
                try {
                    save(sendMessageVo);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new E2Echoxception(null, e.getMessage());
                }
            } else if (MessageType.AUDIO.equals(sendMessageVo.getType()) ||
                    MessageType.FILE.equals(sendMessageVo.getType()) ||
                    MessageType.PICTURE.equals(sendMessageVo.getType()) ||
                    MessageType.VIDEO.equals(sendMessageVo.getType())) {
                // 处理文件类型的数据

                // 将数据转为对应类型
                File receiveFile = jsonUtil.typeCast(sendMessageVo.getSendData(), File.class);

                // 下载文件
                File file = fileService.download(receiveFile);

                // 序列化
                sendMessageVo.setData(objectMapper.writeValueAsString(file));

                // 保存
                save(sendMessageVo);
            } else {
                // 其他类型消息
                throw new UnsupportedOperationException("Unimplemented type %d".formatted(sendMessageVo.getType()));
            }
        } catch (E2Echoxception e) {
            e.printStackTrace();
            throw new E2Echoxception(e.getCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

}
