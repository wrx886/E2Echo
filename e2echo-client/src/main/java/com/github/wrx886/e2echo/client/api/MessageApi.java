package com.github.wrx886.e2echo.client.api;

import java.util.Collection;
import java.util.List;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.PageData;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.util.ApiUtil;
import com.github.wrx886.e2echo.ecc.EccMessage;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

/**
 * 消息接口。
 *
 * <p>对应服务端的 {@code /message} 系列接口：保存、按 ID 查询、按条件分页查询。消息的加密与签名由
 * {@link com.github.wrx886.e2echo.ecc.Ecc} 完成，本类只负责传输。</p>
 */
@Component
@RequiredArgsConstructor
public class MessageApi {

    /**
     * WebClient，其 baseUrl 为登入时填写的服务器地址。
     */
    private final WebClient webClient;

    /**
     * 时间戳接口，请求前校验本机与服务器的时间。
     */
    private final TimestampApi timestampApi;

    /**
     * 保存消息。
     *
     * <p>服务端会校验签名与发送时间，调用前需要先给消息签名。</p>
     *
     * @param message 待保存的消息
     * @return 保存后的消息
     * @throws E2EchoException 请求失败，或服务端返回失败状态
     */
    public EccMessage save(EccMessage message) {
        timestampApi.checkTime();

        return ApiUtil.apiGet(() -> webClient.post()
                .uri("message")
                .bodyValue(message)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<EccMessage>>() {
                })
                .block());
    }

    /**
     * 根据 ID 查询消息。
     *
     * @param id 消息 ID
     * @return 消息
     * @throws E2EchoException 请求失败，或服务端返回失败状态（例如消息不存在）
     */
    public EccMessage getById(String id) {
        timestampApi.checkTime();

        return ApiUtil.apiGet(() -> webClient.get()
                .uri("message/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<EccMessage>>() {
                })
                .block());
    }

    /**
     * 按条件分页查询消息列表。
     *
     * <p>过滤条件为空时不参与过滤；结果按消息 ID 排序，因此 {@code asc} 从老到新、{@code desc}
     * 从新到老。</p>
     *
     * @param fromList       发送者公钥列表，为空表示不过滤
     * @param toList         接收者信息列表，为空表示不过滤
     * @param channel        消息通道，为空表示不过滤
     * @param startTimestamp 起始时间戳（毫秒，含），为空表示不限制
     * @param endTimestamp   结束时间戳（毫秒，含），为空表示不限制
     * @param startId        起始消息 ID，仅返回 ID 大于该值的消息，为空表示不限制
     * @param order          排序方向，{@code asc} 从老到新、{@code desc} 从新到老，为空时从老到新
     * @param pageNum        页码，从 1 开始
     * @param pageSize       每页条数
     * @return 分页消息列表
     * @throws E2EchoException 请求失败，或服务端返回失败状态（例如页码或排序方向非法）
     */
    public PageData<EccMessage> list(List<String> fromList, List<String> toList, String channel,
                                     String startTimestamp, String endTimestamp, String startId, String order,
                                     int pageNum, int pageSize) {
        timestampApi.checkTime();
        return ApiUtil.apiGet(() -> webClient.get()
                .uri(builder -> {
                    builder.path("message");
                    addParam(builder, "fromList", fromList);
                    addParam(builder, "toList", toList);
                    addParam(builder, "channel", channel);
                    addParam(builder, "startTimestamp", startTimestamp);
                    addParam(builder, "endTimestamp", endTimestamp);
                    addParam(builder, "startId", startId);
                    addParam(builder, "order", order);
                    builder.queryParam("pageNum", pageNum);
                    builder.queryParam("pageSize", pageSize);
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<PageData<EccMessage>>>() {
                })
                .block());
    }

    /**
     * 添加查询参数，空白字符串与空集合不参与查询。
     *
     * @param builder 地址构造器
     * @param name    参数名
     * @param value   参数值，字符串或集合
     */
    private static void addParam(UriBuilder builder, String name, Object value) {

        if (value instanceof Collection<?> values) {
            if (!values.isEmpty()) {
                builder.queryParam(name, values.toArray());
            }
        } else if (value instanceof String text) {
            if (!text.isBlank()) {
                builder.queryParam(name, text);
            }
        }
    }

}
