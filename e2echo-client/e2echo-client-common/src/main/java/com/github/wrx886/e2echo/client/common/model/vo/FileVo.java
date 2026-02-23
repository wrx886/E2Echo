package com.github.wrx886.e2echo.client.common.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件信息")
public class FileVo {

    @Schema(description = "文件 ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

}
