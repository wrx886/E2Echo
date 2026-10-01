package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.entity.AesKey;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.handler.ChatGroupKeyMessageHandler;
import com.github.wrx886.e2echo.client.util.IdUtil;
import com.github.wrx886.e2echo.ecc.Ecc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * 群聊管理业务逻辑层。
 *
 * <p>负责群标识生成与群密钥轮换：群标识由“群主公钥 + 随机 ID”拼成，从群标识前缀就能看出群主是谁，
 * 因此只有群主能管理这个群；轮换密钥时会生成新的 AES 密钥并把分发给全部成员。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    /**
     * AES 密钥业务对象，用于群密钥的读写。
     */
    private final AesKeyService aesKeyService;

    /**
     * 群密钥消息处理器，用于把密钥分发给成员。
     */
    private final ChatGroupKeyMessageHandler chatGroupKeyMessageHandler;

    /**
     * 生成一个新的群标识。
     *
     * <p>群标识以当前用户公钥开头，所以这个群只有自己能管理。</p>
     *
     * @return 群标识
     */
    public String generateGroupId() {
        return currentOwner() + IdUtil.newId();
    }

    /**
     * 轮换群密钥：生成一个新的 AES 密钥并分发给全部成员。
     *
     * @param group 群标识
     * @throws E2EchoException 自己不是群主，或密钥创建失败
     */
    public void updateGroupKey(String group) {
        // 判断是否为群主
        if (!currentOwner().equals(group.substring(0, currentOwner().length()))) {
            throw new E2EchoException("非群主，禁止管理！");
        }

        // 更新一个新密钥
        AesKey aesKey = new AesKey();
        aesKey.setObj(group);
        aesKey.setPublishTime(System.currentTimeMillis());
        try {
            aesKey.setAesKey(Ecc.generateAesKeyAsHex());
        } catch (Exception e) {
            log.error("密钥创建失败！", e);
            throw new E2EchoException("密钥创建失败！");
        }
        aesKeyService.save(aesKey);

        // 分发
        chatGroupKeyMessageHandler.send(group);
    }

}
