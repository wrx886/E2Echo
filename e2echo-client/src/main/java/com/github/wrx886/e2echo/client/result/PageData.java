package com.github.wrx886.e2echo.client.result;

import java.util.List;

/**
 * 分页结果。
 *
 * <p>对应服务端 {@code Page} 序列化后的 JSON，只保留客户端关心的字段；服务端返回的
 * {@code pageable}、{@code sort}、{@code numberOfElements} 等字段会被忽略。</p>
 *
 * <p>不用 Spring Data 的 {@code Page} 接收，是因为 {@code Page} 与 {@code PageImpl} 都没有
 * 反序列化构造器，直接接收会抛 {@code Cannot construct instance of Page}。</p>
 *
 * @param content       当前页的数据
 * @param number        当前页码，从 0 开始
 * @param size          每页条数
 * @param totalElements 总条数
 * @param <T>           数据类型
 */
public record PageData<T>(List<T> content, int number, int size, long totalElements) {
}
