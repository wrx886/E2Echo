package com.github.wrx886.e2echo.server.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wrx886.e2echo.server.common.LoginHold;
import com.github.wrx886.e2echo.server.model.entity.User;
import com.github.wrx886.e2echo.server.model.vo.sign.SignCodeVo;
import com.github.wrx886.e2echo.server.model.vo.user.UserChangeVo;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.web.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "用户管理")
@RequestMapping("server/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "获取登入用户信息")
    @GetMapping("info")
    public Result<User> info() {
        return Result.ok(LoginHold.getUser());
    }

    @Operation(summary = "换绑业务：获取原手机号的验证码")
    @GetMapping("getOldCodeOfChange")
    public Result<Void> getOldCodeOfChange() {
        userService.getOldCodeOfChange(LoginHold.getUser());
        return Result.ok();
    }

    @Operation(summary = "换绑业务：换取新手机验证码")
    @GetMapping("getNewCodeOfChange")
    public Result<Void> getNewCodeOfChage(String phone) {
        userService.getNewCodeOfChage(phone);
        return Result.ok();
    }

    @Operation(summary = "换绑业务：获取签名码")
    @GetMapping("getSignCodeOfChange")
    public Result<SignCodeVo> getSignCodeOfChange() {
        return Result.ok(userService.getSignCodeOfChange());
    }

    @Operation(summary = "换绑业务")
    @PostMapping("change")
    public Result<Void> change(@RequestBody UserChangeVo changeVo) {
        userService.change(LoginHold.getUser(), changeVo);
        return Result.ok();
    }

}
