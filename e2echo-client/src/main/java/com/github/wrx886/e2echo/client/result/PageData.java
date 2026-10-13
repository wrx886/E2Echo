package com.github.wrx886.e2echo.client.result;

import java.util.List;

/**
 * 分页结果。
 *
 * <p>与 Spring Data 的 {@code Page} 一样按 {@code VIA_DTO} 的形式提供分页信息：数据在 {@code content}，
 * 页码等信息收在 {@code page} 对象里，形如
 * {@code {"content":[...],"page":{"size":10,"number":0,"totalElements":42,"totalPages":5}}}。</p>
 *
 * <p>不用 Spring Data 的 {@code Page} 接收服务端响应，是因为 {@code Page} 与 {@code PageImpl} 都没有
 * 反序列化构造器，直接接收会抛 {@code Cannot construct instance of Page}。</p>
 *
 * @param content 当前页的数据
 * @param page    分页信息
 * @param <T>     数据类型
 */
public record PageData<T>(List<T> content, PageMetadata page) {

    /**
     * 分页信息。
     *
     * @param size          每页条数
     * @param number        当前页码，从 0 开始
     * @param totalElements 总条数
     * @param totalPages    总页数
     */
    public record PageMetadata(int size, int number, long totalElements, int totalPages) {
    }

}
