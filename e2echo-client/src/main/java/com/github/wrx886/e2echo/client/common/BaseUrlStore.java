package com.github.wrx886.e2echo.client.common;

public final class BaseUrlStore {

    private static String baseUrl;

    public static synchronized String getBaseUrl() {
        return baseUrl;
    }

    public static synchronized void setBaseUrl(String baseUrl) {
        BaseUrlStore.baseUrl = baseUrl;
    }

}
