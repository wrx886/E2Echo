package com.github.wrx886.e2echo.client.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.HttpResult;
import com.github.wrx886.e2echo.client.model.api.SignCodeApiVo;
import com.github.wrx886.e2echo.client.model.api.UserApiVo;
import com.github.wrx886.e2echo.client.model.api.UserChangeApiVo;
import com.github.wrx886.e2echo.client.util.HttpUtil;
import com.github.wrx886.e2echo.client.util.JsonUtil;

// User API
@Component
public class UserApi {

    @Autowired
    private JsonUtil jsonUtil;

    @Autowired
    private HttpUtil httpUtil;

    // 换绑业务
    public void change(String baseUrl, String accessToken, UserChangeApiVo userChangeApiVo) {
        httpUtil.post(baseUrl, "/server/user/change",
                Map.of("access-token", accessToken), userChangeApiVo);
    }

    // 获取登入用户信息
    public UserApiVo info(String baseUrl, String accessToken) {
        HttpResult<?> httpResult = httpUtil.get(baseUrl, "/server/user/info",
                Map.of("access-token", accessToken), null);
        return jsonUtil.typeCast(httpResult.getData(), UserApiVo.class);
    }

    // 换绑业务：获取签名码
    public SignCodeApiVo getSignCodeOfChange(String baseUrl, String accessToken) {
        HttpResult<?> httpResult = httpUtil.get(baseUrl, "/server/user/getSignCodeOfChange",
                Map.of("access-token", accessToken), null);
        return jsonUtil.typeCast(httpResult.getData(), SignCodeApiVo.class);
    }

    // 换绑业务：获取原手机号的验证码
    public void getOldCodeOfChange(String baseUrl, String accessToken) {
        httpUtil.get(baseUrl, "/server/user/getOldCodeOfChange",
                Map.of("access-token", accessToken), null);
    }

    // 换绑业务：获取新手机号的验证码
    public void getNewCodeOfChange(String baseUrl, String accessToken, String phone) {
        httpUtil.get(baseUrl, "/server/user/getNewCodeOfChange",
                Map.of("access-token", accessToken),
                Map.of("phone", phone));
    }

}
