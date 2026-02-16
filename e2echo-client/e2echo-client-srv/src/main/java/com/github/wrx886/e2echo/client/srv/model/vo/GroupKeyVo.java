package com.github.wrx886.e2echo.client.srv.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GroupKeyVo {

    @Schema(description = "群聊 UUID")
    private String groupUuid;

    @Schema(description = "时间戳(int64)")
    private String timestamp;

    @Schema(description = "新的AesKey")
    private String aesKey;

}
