package com.github.wrx886.e2echo.server.test.web.controller;

import org.junit.jupiter.api.Assertions;
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
import com.github.wrx886.e2echo.server.model.entity.User;
import com.github.wrx886.e2echo.server.model.vo.sign.SignCodeVo;
import com.github.wrx886.e2echo.server.model.vo.user.UserChangeVo;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.test.common.ResultUtil;
import com.github.wrx886.e2echo.server.test.common.TestUserInfo;
import com.github.wrx886.e2echo.server.util.EccUtil;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUserInfo testUserInfo;

    @Autowired
    private ResultUtil resultUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void infoTest() throws Exception {
        // 查询登入用户信息
        Result<User> result = resultUtil.getResultFromMockResponse(mockMvc
                .perform(MockMvcRequestBuilders.get("/server/user/info")
                        .header("access-token", testUserInfo.getAccessToken()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), User.class);

        // 打印用户信息
        System.out.println(result.getData());
        assert result.getData() != null;
    }

    @Test
    public void change() throws Exception {
        // 获取原手机号的验证码
        Result<Void> getOldCodeOfChangeResult = resultUtil.getResultFromMockResponse(mockMvc
                .perform(MockMvcRequestBuilders.get("/server/user/getOldCodeOfChange")
                        .header("access-token", testUserInfo.getAccessToken()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), Void.class);
        assert ResultCodeEnum.OK.getCode().equals(getOldCodeOfChangeResult.getCode()) ||
                ResultCodeEnum.USER_PHONE_CODE_SEND_TOO_OFTEN.getCode().equals(getOldCodeOfChangeResult.getCode());

        // 获取原手机号的验证码
        String oldCode = stringRedisTemplate.opsForValue()
                .get(RedisPrefix.USER_CHANGE_OLD_CODE_PREFIX + testUserInfo.getPhone());

        // 获取新手机号的验证码
        Result<Void> getNewCodeOfChangeResult = resultUtil.getResultFromMockResponse(mockMvc
                .perform(MockMvcRequestBuilders.get("/server/user/getNewCodeOfChange")
                        .header("access-token", testUserInfo.getAccessToken())
                        .param("phone", testUserInfo.getPhone()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), Void.class);
        assert ResultCodeEnum.OK.getCode().equals(getNewCodeOfChangeResult.getCode()) ||
                ResultCodeEnum.USER_PHONE_CODE_SEND_TOO_OFTEN.getCode().equals(getNewCodeOfChangeResult.getCode());

        // 获取新手机的验证码
        String newCode = stringRedisTemplate.opsForValue()
                .get(RedisPrefix.USER_CHANGE_NEW_CODE_PREFIX + testUserInfo.getPhone());

        // 获取签名码
        Result<SignCodeVo> getSignCodeOfChangeResult = resultUtil.getResultFromMockResponse(mockMvc
                .perform(MockMvcRequestBuilders.get("/server/user/getSignCodeOfChange")
                        .header("access-token", testUserInfo.getAccessToken()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), SignCodeVo.class);
        Assertions.assertEquals(getSignCodeOfChangeResult.getCode(), ResultCodeEnum.OK.getCode());

        // 提取签名码
        SignCodeVo signCodeVo = getSignCodeOfChangeResult.getData();

        // 构建换绑参数
        UserChangeVo userChangeVo = new UserChangeVo();
        userChangeVo.setNewPhone(testUserInfo.getPhone());
        userChangeVo.setNewCode(newCode);
        userChangeVo.setOldCode(oldCode);
        userChangeVo.setSignCodeKey(signCodeVo.getKey());
        userChangeVo.setSignCodeValue(EccUtil.sign(signCodeVo.getValue(), testUserInfo.getPrivateKey()));

        // 运行换绑业务
        Result<Void> changeResult = resultUtil.getResultFromMockResponse(mockMvc
                .perform(MockMvcRequestBuilders.post("/server/user/change")
                        .header("access-token", testUserInfo.getAccessToken())
                        .content(objectMapper.writeValueAsString(userChangeVo))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), Void.class);
        Assertions.assertEquals(changeResult.getCode(), ResultCodeEnum.OK.getCode());

    }

}
