package com.github.wrx886.e2echo.client.gui.modol.vo;

import lombok.Data;

@Data
public class EditVo {

    // 别名
    private String alias;

    // 公钥或群聊UUID
    private String publicKeyHex;

    // 是否是群聊
    private Boolean group;

}
