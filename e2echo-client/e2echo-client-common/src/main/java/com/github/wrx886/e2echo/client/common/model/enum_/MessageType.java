package com.github.wrx886.e2echo.client.common.model.enum_;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum MessageType implements IEnum<Integer> {
    // 文本
    TEXT(1),
    // 图片
    PICTURE(2),
    // 音频
    AUDIO(3),
    // 视频
    VIDEO(4),
    // 文件
    FILE(5),
    // 不支持的数据类型
    UNSUPPORTED(6),

    // 群聊：KEY更新
    GROUP_KEY_UPDATE(7),

    // 群聊密钥共享机制
    GROUP_KEY_SHARED(8),
    ;

    @Getter
    private final Integer value;

}
