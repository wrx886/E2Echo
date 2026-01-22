package com.github.wrx886.e2echo.server.util;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.model.entity.EccMessage;

public final class EccMessageUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证签名
     * 
     * @param eccMessage 待验证的 ECC 消息
     * @return true 验证通过，false 验证失败
     */
    public static boolean verify(EccMessage eccMessage) {
        try {
            // 验证签名
            return EccUtil.verify(
                    objectMapper.writeValueAsString(toMap(eccMessage)),
                    eccMessage.getSignature(), eccMessage.getFromPublicKeyHex());
        } catch (Exception e) {
            // 出现异常，返回失败
            return false;
        }
    }

    /**
     * 将 ECC 消息转为 Map
     * 
     * @param eccMessage ECC 消息
     * @return Map
     */
    private static LinkedHashMap<String, String> toMap(EccMessage eccMessage) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("uuid", eccMessage.getUuid());
        map.put("timestamp", eccMessage.getTimestamp());
        map.put("fromPublicKeyHex", eccMessage.getFromPublicKeyHex());
        map.put("toPublicKeyHex", eccMessage.getToPublicKeyHex());
        map.put("data", eccMessage.getData());
        return map;
    }

}
