package com.github.wrx886.e2echo.server.web.service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.server.common.RedisPrefix;
import com.github.wrx886.e2echo.server.model.entity.User;
import com.github.wrx886.e2echo.server.model.vo.sign.SignCodeVo;
import com.github.wrx886.e2echo.server.model.vo.user.UserChangeVo;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.util.EccUtil;
import com.github.wrx886.e2echo.server.web.mapper.UserMapper;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private SmsService smsService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 换绑业务：获取原手机号的验证码
    public void getOldCodeOfChange(User user) {
        // 生成 key
        String key = RedisPrefix.USER_CHANGE_OLD_CODE_PREFIX + user.getPhone();

        // 判断 key 是否存在
        if (stringRedisTemplate.hasKey(key)) {
            // 验证码发送过于频繁
            throw new E2EchoException(ResultCodeEnum.USER_PHONE_CODE_SEND_TOO_OFTEN);
        }

        // 生成手机验证码
        String code = Integer.toString(new Random().nextInt((int) 1e6));

        // 存储到 Redis
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

        // 发送手机验证码
        smsService.sendCode(user.getPhone(), code, "换绑业务");
    }

    // 换绑业务：换取新手机验证码
    public void getNewCodeOfChage(String phone) {
        // 生成 key
        String key = RedisPrefix.USER_CHANGE_NEW_CODE_PREFIX + phone;

        // 判断 key 是否存在
        if (stringRedisTemplate.hasKey(key)) {
            // 验证码发送过于频繁
            throw new E2EchoException(ResultCodeEnum.USER_PHONE_CODE_SEND_TOO_OFTEN);
        }

        // 生成手机验证码
        String code = Integer.toString(new Random().nextInt((int) 1e6));

        // 存储到 Redis
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

        // 发送手机验证码
        smsService.sendCode(phone, code, "换绑业务");
    }

    // 换绑业务：获取签名码
    public SignCodeVo getSignCodeOfChange() {
        // 生成 key
        String key = UUID.randomUUID().toString().replaceAll("-", "");

        // 生成 value
        String value = UUID.randomUUID().toString().replaceAll("-", "")
                + UUID.randomUUID().toString().replaceAll("-", "");

        // 存储到 redis
        stringRedisTemplate.opsForValue().set(RedisPrefix.USER_CHANGE_SIGN_CODE_PREFIX + key, value, 30,
                TimeUnit.SECONDS);

        // 返回
        return new SignCodeVo(key, value);
    }

    // 换绑业务
    public void change(User user, UserChangeVo changeVo) {
        // 验证手机号是否存在
        if (user.getPhone() == null || changeVo.getNewPhone() == null) {
            throw new E2EchoException(ResultCodeEnum.USER_PHONE_EMPTY);
        }

        // 验证手机验证码是否存在
        if (changeVo.getNewCode() == null || changeVo.getOldCode() == null) {
            throw new E2EchoException(ResultCodeEnum.USER_PHONE_CODE_EMPTY);
        }

        // 验证签名码是否存在
        if (changeVo.getSignCodeKey() == null || changeVo.getSignCodeValue() == null) {
            throw new E2EchoException(ResultCodeEnum.USER_SIGN_CODE_EMPTY);
        }

        // 验证旧手机号的验证码是否过期
        String oldCodeKey = RedisPrefix.USER_CHANGE_OLD_CODE_PREFIX + user.getPhone();
        String oldCode = stringRedisTemplate.opsForValue().get(oldCodeKey);
        if (oldCode == null) {
            throw new E2EchoException(ResultCodeEnum.USER_PHONE_CODE_EXPIRED);
        }

        // 验证旧手机验证码是否正确
        if (!oldCode.equals(changeVo.getOldCode())) {
            throw new E2EchoException(ResultCodeEnum.USER_PHONE_CODE_ERROR);
        }

        // 验证新手机号的验证码是否过期
        String newCodeKey = RedisPrefix.USER_CHANGE_NEW_CODE_PREFIX + changeVo.getNewPhone();
        String newCode = stringRedisTemplate.opsForValue().get(newCodeKey);
        if (newCode == null) {
            throw new E2EchoException(ResultCodeEnum.USER_PHONE_CODE_EXPIRED);
        }

        // 验证新手机验证码是否正确
        if (!newCode.equals(changeVo.getNewCode())) {
            throw new E2EchoException(ResultCodeEnum.USER_PHONE_CODE_ERROR);
        }

        // 验证签名码是否过期
        String signCodeKey = RedisPrefix.USER_CHANGE_SIGN_CODE_PREFIX + changeVo.getSignCodeKey();
        String signCode = stringRedisTemplate.opsForValue().get(signCodeKey);
        if (signCode == null) {
            throw new E2EchoException(ResultCodeEnum.USER_SIGN_CODE_EXPIRED);
        }

        // 验证签名是否正确
        try {
            if (!EccUtil.verify(signCode, changeVo.getSignCodeValue(), user.getPublicKey())) {
                throw new E2EchoException(ResultCodeEnum.USER_SIGN_CODE_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new E2EchoException(ResultCodeEnum.USER_SIGN_CODE_ERROR);
        }

        // 删除验证码
        stringRedisTemplate.delete(newCodeKey);
        stringRedisTemplate.delete(oldCodeKey);
        stringRedisTemplate.delete(signCodeKey);

        // 进行换绑
        user.setPhone(changeVo.getNewPhone());
        updateById(user);
    }

}
