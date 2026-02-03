package com.github.wrx886.e2echo.client.srv.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "全局统一返回结果类")
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    @Schema(description = "返回码")
    private String code;

    @Schema(description = "返回消息")
    private String message;

    @Schema(description = "返回数据")
    private T data;

}
