package com.github.wrx886.e2echo.client.controller;

import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.util.CommonUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口。
 *
 * <p>客户端自己的 HTTP 接口，供前端使用：查询当前登入用户自身的身份。</p>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    /**
     * 查询当前登入用户的公钥。
     *
     * @return 当前登入用户的 secp256k1 公钥（RAW HEX 格式）
     */
    @GetMapping("current")
    public Result<String> current() {
        return Result.ok(CommonUtil.currentOwner());
    }

}
