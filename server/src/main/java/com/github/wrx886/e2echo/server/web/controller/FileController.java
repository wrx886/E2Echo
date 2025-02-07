package com.github.wrx886.e2echo.server.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.web.service.FileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "文件管理")
@RequestMapping("server/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Operation(summary = "上传文件")
    @PostMapping("upload")
    public Result<String> upload(@RequestBody MultipartFile file) throws Exception {
        return Result.ok(fileService.upload(file));
    }

}
