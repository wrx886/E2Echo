package com.github.wrx886.e2echo.client.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.client.api.LoginApi;
import com.github.wrx886.e2echo.client.api.MessageWebSocketApi;
import com.github.wrx886.e2echo.client.mapper.LoginUserMapper;
import com.github.wrx886.e2echo.client.mapper.MessageMapper;
import com.github.wrx886.e2echo.client.model.api.LoginApiVo;
import com.github.wrx886.e2echo.client.model.api.SignCodeApiVo;
import com.github.wrx886.e2echo.client.model.entity.LoginUser;
import com.github.wrx886.e2echo.client.store.LoginUserStore;
import com.github.wrx886.e2echo.client.store.MessageStore;
import com.github.wrx886.e2echo.client.util.EccUtil;

// 登入用户服务
@Service
public class LoginUserService extends ServiceImpl<LoginUserMapper, LoginUser> {

    @Autowired
    private LoginApi loginApi;

    @Autowired
    private LoginUserStore loginUserStore;

    @Autowired
    private MessageWebSocketApi messageWebSocketApi;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageStore messageStore;

    // 登入
    public void login(
            String baseUrl,
            String phone,
            String phoneCode,
            String publicKey,
            String privateKey) {
        // 构建登入参数
        LoginApiVo loginApiVo = new LoginApiVo();
        loginApiVo.setPhone(phone);
        loginApiVo.setPhoneCode(phoneCode);
        loginApiVo.setPublicKey(publicKey);

        // 获取和处理签名码
        SignCodeApiVo signCodeApiVo = loginApi.getSignCode(baseUrl);
        signCodeApiVo.setValue(EccUtil.sign(signCodeApiVo.getValue(), privateKey));

        // 传入登入参数
        loginApiVo.setSignCodeKey(signCodeApiVo.getKey());
        loginApiVo.setSignCodeValue(signCodeApiVo.getValue());

        // 登入并获取 access-token
        String accessToken = loginApi.login(baseUrl, loginApiVo);

        // 注册到登入用户表
        LoginUser loginUser = putLoginUserByPublicKey(publicKey);

        // 写入到登入用户信息
        loginUserStore.setId(loginUser.getId());
        loginUserStore.setBaseUrl(baseUrl);
        loginUserStore.setPublicKey(publicKey);
        loginUserStore.setPrivateKey(privateKey);
        loginUserStore.setAccessToken(accessToken);

        // 写入 MessageWebSocketApi
        messageWebSocketApi.setBaseUrl(baseUrl);

        // 从数据库中获取最新刷时间
        Date lastDate = messageMapper.getLastSendTime(loginUserStore.getId());
        if(lastDate != null) {
            messageStore.setLastUpdateTime(lastDate);
        }
    }

    // 获取验证码
    public void getPhoneCode(String baseUrl, String phone) {
        loginApi.getPhoneCode(baseUrl, phone);
    }

    // 注册用户
    public LoginUser putLoginUserByPublicKey(String publicKey) {
        // 根据公钥查询
        LoginUser loginUser = getOne(new LambdaQueryWrapper<LoginUser>()
                .eq(LoginUser::getPublicKey, publicKey));
        if (loginUser == null) {
            // 插入数据
            loginUser = new LoginUser();
            loginUser.setPublicKey(publicKey);
            save(loginUser);
        }
        return loginUser;
    }

}
