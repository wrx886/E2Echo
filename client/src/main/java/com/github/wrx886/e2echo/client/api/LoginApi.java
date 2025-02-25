package com.github.wrx886.e2echo.client.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.HttpResult;
import com.github.wrx886.e2echo.client.model.api.LoginApiVo;
import com.github.wrx886.e2echo.client.model.api.SignCodeApiVo;
import com.github.wrx886.e2echo.client.util.HttpUtil;
import com.github.wrx886.e2echo.client.util.JsonUtil;

// Login API
@Component
public class LoginApi {

    @Autowired
    private JsonUtil jsonUtil;

    @Autowired
    private HttpUtil httpUtil;

    // 登入（或注册）并获取 access-token
    public String login(String baseUrl, LoginApiVo loginApiVo) {
        HttpResult<?> httpResult = httpUtil.post(baseUrl, "/server/login/login", null, loginApiVo);
        return jsonUtil.typeCast(httpResult.getData(), String.class);
    }

    // 获取签名
    public SignCodeApiVo getSignCode(String baseUrl) {
        HttpResult<?> httpResult = httpUtil.get(baseUrl, "/server/login/getSignCode", null, null);
        return jsonUtil.typeCast(httpResult.getData(), SignCodeApiVo.class);
    }

    // 获取手机验证码
    public void getPhoneCode(String baseUrl, String phone) {
        httpUtil.get(baseUrl, "/server/login/getPhoneCode", null,
                Map.of("phone", phone));
    }

}
