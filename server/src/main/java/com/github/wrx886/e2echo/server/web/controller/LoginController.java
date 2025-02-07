package com.github.wrx886.e2echo.server.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wrx886.e2echo.server.model.vo.login.LoginVo;
import com.github.wrx886.e2echo.server.model.vo.sign.SignCodeVo;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.web.service.LoginService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "用户登入相关接口")
@RequestMapping("server/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Operation(summary = "获取手机验证码")
    @GetMapping("getPhoneCode")
    public Result<Void> getPhoneCode(String phone) {
        loginService.getPhoneCode(phone);
        return Result.ok();
    }

    @Operation(summary = "获取签名码")
    @GetMapping("getSignCode")
    public Result<SignCodeVo> getSignCode() {
        return Result.ok(loginService.getSignCode());
    }

    @Operation(summary = "登入（或注册）并获取 access-token")
    @PostMapping("login")
    public Result<String> login(@RequestBody LoginVo loginVo) {
        return Result.ok(loginService.login(loginVo));
    }

}
