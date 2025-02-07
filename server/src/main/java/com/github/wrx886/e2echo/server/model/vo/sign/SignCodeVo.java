package com.github.wrx886.e2echo.server.model.vo.sign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "签名码")
public class SignCodeVo {

    @Schema(description = "签名码的 key")
    private String key;

    @Schema(description = "签名码的值，需要使用私钥进行加密后返回")
    private String value;

}
