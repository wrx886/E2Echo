package com.github.wrx886.e2echo.client.util;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.ecc.Ecc;

/**
 * 通用工具类。
 */
public final class CommonUtil {

    /**
     * 获取当前登入用户的公钥。
     *
     * @return 当前登入用户的公钥
     * @throws E2EchoException 尚未登入
     */
    public static String currentOwner() {
        String owner = Ecc.getPublicKey();
        if (owner == null) {
            throw new E2EchoException("尚未登入！");
        }
        return owner;
    }

}
