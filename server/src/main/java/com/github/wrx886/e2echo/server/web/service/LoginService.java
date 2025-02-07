package com.github.wrx886.e2echo.server.web.service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wrx886.e2echo.server.common.RedisPrefix;
import com.github.wrx886.e2echo.server.model.entity.User;
import com.github.wrx886.e2echo.server.model.vo.login.LoginVo;
import com.github.wrx886.e2echo.server.model.vo.sign.SignCodeVo;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.util.EccUtil;
import com.github.wrx886.e2echo.server.util.JwtUtils;
import com.github.wrx886.e2echo.server.web.map.UserSessionMap;

import io.jsonwebtoken.Claims;
import lombok.Getter;

@Service
public class LoginService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SmsService smsService;

    @Autowired
    private UserService userService;

    // 存储用户和 Session ID 之间的关系
    @Getter
    private final UserSessionMap userSessionMap = new UserSessionMap();

    @Autowired
    private JwtUtils jwtUtils;

    public void getPhoneCode(String phone) {
        // 生成 key
        String key = RedisPrefix.LOGIN_PHONE_CODE_PREFIX + phone;

        // 判断 key 是否存在
        if (stringRedisTemplate.hasKey(key)) {
            // 验证码发送过于频繁
            throw new E2EchoException(ResultCodeEnum.LOGIN_PHONE_CODE_SEND_TOO_OFTEN);
        }

        // 生成手机验证码
        String code = Integer.toString(new Random().nextInt((int) 1e6));

        // 存储到 Redis
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

        // 发送手机验证码
        smsService.sendCode(phone, code, "登入业务");
    }

    public SignCodeVo getSignCode() {
        // 生成 key
        String key = UUID.randomUUID().toString().replaceAll("-", "");

        // 生成 value
        String value = UUID.randomUUID().toString().replaceAll("-", "")
                + UUID.randomUUID().toString().replaceAll("-", "");

        // 存储到 redis
        stringRedisTemplate.opsForValue().set(RedisPrefix.LOGIN_SIGN_CODE_PREFIX + key, value, 30, TimeUnit.SECONDS);

        // 返回
        return new SignCodeVo(key, value);
    }

    public String login(LoginVo loginVo) {
        // 验证手机号是否存在
        if (loginVo.getPhone() == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_PHONE_EMPTY);
        }

        // 验证手机验证码是否存在
        if (loginVo.getPhoneCode() == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_PHONE_CODE_EMPTY);
        }

        // 验证签名码是否存在
        if (loginVo.getSignCodeKey() == null || loginVo.getSignCodeValue() == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_SIGN_CODE_EMPTY);
        }

        // 验证公钥是否存在
        if (loginVo.getPublicKey() == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_PUBLIC_KEY_EMPTY);
        }

        // 验证手机验证码是否过期
        String phoneCodeKey = RedisPrefix.LOGIN_PHONE_CODE_PREFIX + loginVo.getPhone();
        String phoneCode = stringRedisTemplate.opsForValue().get(phoneCodeKey);
        if (phoneCode == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_PHONE_CODE_EXPIRED);
        }

        // 验证手机验证码是否正确
        if (!phoneCode.equals(loginVo.getPhoneCode())) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_PHONE_CODE_ERROR);
        }

        // 验证签名码是否过期
        String signCodeKey = RedisPrefix.LOGIN_SIGN_CODE_PREFIX + loginVo.getSignCodeKey();
        String signCode = stringRedisTemplate.opsForValue().get(signCodeKey);
        if (signCode == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_SIGN_CODE_EXPIRED);
        }

        // 验证签名是否正确
        try {
            if (!EccUtil.verify(signCode, loginVo.getSignCodeValue(), loginVo.getPublicKey())) {
                throw new E2EchoException(ResultCodeEnum.LOGIN_SIGN_CODE_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new E2EchoException(ResultCodeEnum.LOGIN_SIGN_CODE_ERROR);
        }

        // 获取用户信息
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, loginVo.getPhone()));
        if (user == null) {
            // 注册新用户
            user = new User();
            user.setPhone(loginVo.getPhone());
            user.setPublicKey(loginVo.getPublicKey());
            userService.save(user);
        } else {
            // 比较公钥是否匹配
            if (!user.getPublicKey().equals(loginVo.getPublicKey())) {
                throw new E2EchoException(ResultCodeEnum.LOGIN_USER_INFO_NOT_MATCH);
            }
        }

        // 生成 token 并返回
        stringRedisTemplate.delete(phoneCodeKey);
        stringRedisTemplate.delete(signCodeKey);
        return jwtUtils.createLoginUserToken(user.getId());
    }

    // WebSocket 登入使用
    public void loginByToken(String sessionId, String accessToken) {
        // 判断 token 是否为空
        if (accessToken == null) {
            // 返回用户未登入
            throw new E2EchoException(ResultCodeEnum.LOGIN_AUTH);
        }
        // 校验通过，则继续执行，否则就会抛出异常
        Claims claims = jwtUtils.parseLoginUserToken(accessToken);
        // 获取用户
        User user = userService.getById(claims.get("userId", Long.class));
        if (user == null) {
            throw new E2EchoException(ResultCodeEnum.LOGIN_USER_DELETED);
        }
        // 写入映射关系
        userSessionMap.addSessionIdAndUserId(sessionId, user.getId());
    }

}
