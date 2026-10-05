package com.github.wrx886.e2echo.client.api;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.result.PageData;
import com.github.wrx886.e2echo.client.result.Result;
import com.github.wrx886.e2echo.client.util.ApiUtil;
import com.github.wrx886.e2echo.client.vo.req.MessageListReqVo;
import com.github.wrx886.e2echo.ecc.EccMessage;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
     * 从新到老。翻页时按排序方向选游标：升序用 {@code startId}（只取更大的），降序用 {@code endId}
     * （只取更小的）。</p>
     *
     * <p>查询条件较多，所以服务端用请求体接收、以 POST 提交（条件里的公钥列表可能很长，放查询串会超
     * 出地址长度限制）。分页深度也有限制：每页不超过 256 条、页码不超过 16，要取更深的数据请用游标
     * 翻页。</p>
     *
     * @param reqVo 分页查询参数
     * @return 分页消息列表
     * @throws E2EchoException 请求失败，或服务端返回失败状态（例如页码、每页条数或排序方向非法）
     */
    public PageData<EccMessage> list(MessageListReqVo reqVo) {
        timestampApi.checkTime();
        return ApiUtil.apiGet(() -> webClient.post()
                .uri("message/list")
                .bodyValue(reqVo)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Result<PageData<EccMessage>>>() {
                })
                .block());
    }

}
