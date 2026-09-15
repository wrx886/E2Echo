package com.github.wrx886.e2echo.client.common;

/**
 * 服务器地址存储器。
 *
 * <p>登入界面在 Spring 容器启动之前运行，此时还没有容器可以访问，所以登入时填写的服务器地址
 * 先保存在这里；容器启动后，{@code WebClient} 的 baseUrl 以及需要访问服务端的业务都从这里读取。</p>
 *
 * <p>登入界面写入、容器读取，因此读写方法都加锁保证可见性。</p>
 */
public final class BaseUrlStore {

    /**
     * 登入时填写的服务器地址，为 {@code null} 表示尚未登入。
     */
    private static String baseUrl;

    /**
     * 私有构造方法，防止外部实例化工具类。
     */
    private BaseUrlStore() {
    }

    /**
     * 获取服务器地址。
     *
     * @return 登入时填写的服务器地址，尚未登入时返回 {@code null}
     */
    public static synchronized String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 保存服务器地址。
     *
     * @param baseUrl 登入时填写的服务器地址
     */
    public static synchronized void setBaseUrl(String baseUrl) {
        BaseUrlStore.baseUrl = baseUrl;
    }

}
