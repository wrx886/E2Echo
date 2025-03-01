package com.github.wrx886.e2echo.client.util;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.CodeEnum;
import com.github.wrx886.e2echo.client.common.E2Echoxception;
import com.github.wrx886.e2echo.client.common.HttpResult;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;

// Http 工具类，用于发送和接收 Http 请求和响应
@Component
public class HttpUtil {

    @Autowired
    private ObjectMapper objectMapper;

    // GET 请求（服务器地址，路径，请求头参数，Parameter参数）
    public HttpResult<?> get(String baseUrl, String path, Map<String, String> headers,
            Map<String, String> parameters) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 1. 封装 uri
            URI baseUri;
            try {
                baseUri = new URI(baseUrl).resolve(path);
            } catch (Exception e) {
                e.printStackTrace();
                throw new E2Echoxception(null, "URI 错误");
            }
            URIBuilder uriBuilder = new URIBuilder(baseUri);
            // 添加请求参数
            if (parameters != null) {
                parameters.forEach((k, v) -> {
                    uriBuilder.addParameter(k, v);
                });
            }
            // 构建 uri
            URI uri = uriBuilder.build();

            // 2. 创建请求方式实例
            HttpGet httpGet = new HttpGet(uri);

            // 4. 设置请求头
            if (headers != null) {
                headers.forEach((k, v) -> {
                    httpGet.addHeader(k, v);
                });
            }
            httpGet.addHeader("Accept", "application/json");

            // 5. 发送请求并获取响应
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    try (HttpEntity entity = response.getEntity(); InputStream content = entity.getContent()) {
                        HttpResult<?> httpResult = objectMapper.readValue(content, HttpResult.class);
                        if (!CodeEnum.OK.getCode().equals(httpResult.getCode())) {
                            throw new E2Echoxception(httpResult.getCode(), httpResult.getMessage());
                        }
                        return httpResult;
                    }
                } else {
                    // 状态码不是 200 时报错
                    throw new RuntimeException("%d %s".formatted(response.getCode(), response.getReasonPhrase()));
                }
            }
        } catch (E2Echoxception e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new E2Echoxception(null, e.getMessage());
        }
    }

    // POST 请求
    public <E> HttpResult<?> post(String baseUrl, String path, Map<String, String> headers, E data) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 1. 封装 URI
            URI uri;
            try {
                uri = new URI(baseUrl).resolve(path);
            } catch (Exception e) {
                e.printStackTrace();
                throw new E2Echoxception(null, "URI 错误");
            }

            // 2. 创建请求实例
            HttpPost httpPost = new HttpPost(uri);

            // 3. 设置请求头
            if (headers != null) {
                headers.forEach((k, v) -> {
                    httpPost.addHeader(k, v);
                });
            }
            httpPost.addHeader("Accept", "application/json");

            // 4. 封装请求体
            if (data != null) {
                if (data instanceof String s) {
                    httpPost.setEntity(new StringEntity(s));
                } else {
                    httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(data)));
                }
                httpPost.addHeader("Accept", "application/json");
                httpPost.addHeader("Content-Type", "application/json");
            }

            // 5. 发送请求并处理返回值
            try (CloseableHttpResponse response = httpClient.execute(httpPost);) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    try (HttpEntity entity = response.getEntity(); InputStream content = entity.getContent()) {
                        HttpResult<?> httpResult = objectMapper.readValue(content, HttpResult.class);
                        if (!CodeEnum.OK.getCode().equals(httpResult.getCode())) {
                            throw new E2Echoxception(httpResult.getCode(), httpResult.getMessage());
                        }
                        return httpResult;
                    }
                } else {
                    // 状态码不是 200 时报错
                    throw new RuntimeException("%d %s".formatted(response.getCode(), response.getReasonPhrase()));
                }
            }
        } catch (E2Echoxception e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new E2Echoxception(null, e.getMessage());
        }
    }

    // 发送文件
    public HttpResult<?> postFile(String baseUrl, String path, Map<String, String> headers, String queryName,
            String filePath) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 1. 封装 URI
            URI uri;
            try {
                uri = new URI(baseUrl).resolve(path);
            } catch (Exception e) {
                e.printStackTrace();
                throw new E2Echoxception(null, "URI 错误");
            }

            // 2. 创建请求实例
            HttpPost httpPost = new HttpPost(uri);

            // 3. 设置请求头
            if (headers != null) {
                headers.forEach((k, v) -> {
                    httpPost.addHeader(k, v);
                });
            }
            httpPost.addHeader("Accept", "application/json");

            // 4. 封装请求体
            File file = new File(filePath);
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.addBinaryBody(
                    queryName,
                    file,
                    ContentType.APPLICATION_OCTET_STREAM,
                    file.getName());
            httpPost.setEntity(multipartEntityBuilder.build());

            // 5. 发送请求并处理返回值
            try (CloseableHttpResponse response = httpClient.execute(httpPost);) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    try (HttpEntity entity = response.getEntity(); InputStream content = entity.getContent()) {
                        HttpResult<?> httpResult = objectMapper.readValue(content, HttpResult.class);
                        if (!CodeEnum.OK.getCode().equals(httpResult.getCode())) {
                            throw new E2Echoxception(httpResult.getCode(), httpResult.getMessage());
                        }
                        return httpResult;
                    }
                } else {
                    // 状态码不是 200 时报错
                    throw new RuntimeException("%d %s".formatted(response.getCode(), response.getReasonPhrase()));
                }
            }
        } catch (E2Echoxception e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new E2Echoxception(null, e.getMessage());
        }
    }
}
