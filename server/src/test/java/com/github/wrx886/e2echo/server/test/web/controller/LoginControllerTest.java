package com.github.wrx886.e2echo.server.test.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.common.RedisPrefix;
import com.github.wrx886.e2echo.server.model.vo.login.LoginVo;
import com.github.wrx886.e2echo.server.model.vo.sign.SignCodeVo;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.test.common.ResultUtil;
import com.github.wrx886.e2echo.server.test.common.TestUserInfo;
import com.github.wrx886.e2echo.server.util.EccUtil;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUserInfo testUserInfo;

    @Autowired
    private ResultUtil resultUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void login() throws Exception {
        LoginVo loginVo = new LoginVo();

        // 1. 获取手机验证码
        loginVo.setPhone(testUserInfo.getPhone());
        loginVo.setPhoneCode(getPhoneCode());

        // 2. 获取签名码并进行签名
        SignCodeVo signCodeVo = getSignCode();
        loginVo.setSignCodeKey(signCodeVo.getKey());
        loginVo.setSignCodeValue(EccUtil.sign(signCodeVo.getValue(), testUserInfo.getPrivateKey()));

        // 3. 填充公钥
        loginVo.setPublicKey(testUserInfo.getPublicKey());

        // 4. 登入并获取 Token
        assert resultUtil.getResultFromMockResponse(mockMvc.perform(MockMvcRequestBuilders.post("/server/login/login")
                .content(objectMapper.writeValueAsString(loginVo))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), String.class).getCode().equals(ResultCodeEnum.OK.getCode());
    }

    // 获取手机验证码
    public String getPhoneCode() throws Exception {
        // 1. 发送 GET 请求
        Result<Void> result = resultUtil.getResultFromMockResponse(mockMvc
                .perform(MockMvcRequestBuilders.get("/server/login/getPhoneCode")
                        .param("phone", testUserInfo.getPhone()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), Void.class);

        // 2. 判断状态码是否为 成功 或 发送过于频繁
        assert ResultCodeEnum.OK.getCode().equals(result.getCode()) ||
                ResultCodeEnum.LOGIN_PHONE_CODE_SEND_TOO_OFTEN.getCode().equals(result.getCode());

        // 3. 拼装 key 从 Redis 中获取验证码
        return stringRedisTemplate.opsForValue()
                .get(RedisPrefix.LOGIN_PHONE_CODE_PREFIX + testUserInfo.getPhone());
    }

    // 获取签名码
    public SignCodeVo getSignCode() throws Exception {
        // 发送请求
        return resultUtil
                .getResultFromMockResponse(
                        mockMvc.perform(MockMvcRequestBuilders.get("/server/login/getSignCode"))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andDo(MockMvcResultHandlers.print())
                                .andReturn().getResponse(),
                        SignCodeVo.class)
                .getData();
    }

}
