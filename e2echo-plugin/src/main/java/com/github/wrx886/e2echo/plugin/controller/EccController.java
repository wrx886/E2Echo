package com.github.wrx886.e2echo.plugin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wrx886.e2echo.plugin.model.entity.EccMessage;
import com.github.wrx886.e2echo.plugin.result.Result;
import com.github.wrx886.e2echo.plugin.service.EccService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "ECC 加解密")
@RestController
@RequestMapping("/ecc")
@AllArgsConstructor
public class EccController {

    private final EccService eccService;

    @Operation(summary = "加密")
    @PostMapping("/encrypt")
    public Result<EccMessage> encrypt(@RequestBody EccMessage eccMessage) {
        return Result.ok(eccService.encrypt(eccMessage));
    }

    @Operation(summary = "解密")
    @PostMapping("/decrypt")
    public Result<EccMessage> decrypt(@RequestBody EccMessage eccMessage) {
        return Result.ok(eccService.decrypt(eccMessage));
    }

    @Operation(summary = "签名")
    @PostMapping("/sign")
    public Result<EccMessage> sign(@RequestBody EccMessage eccMessage) {
        return Result.ok(eccService.sign(eccMessage));
    }

    @Operation(summary = "验证签名")
    @PostMapping("/verify")
    public Result<Boolean> verify(@RequestBody EccMessage eccMessage) {
        return Result.ok(EccService.verify(eccMessage));
    }

    @Operation(summary = "获取公钥")
    @GetMapping("/publicKey")
    public Result<String> publicKey() {
        return Result.ok(eccService.getPublicKey());
    }

}
