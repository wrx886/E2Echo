package com.github.wrx886.e2echo.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.github.wrx886.e2echo.ecc.Ecc;
import com.github.wrx886.e2echo.ecc.EccMessage;
import com.github.wrx886.e2echo.server.entity.Message;
import com.github.wrx886.e2echo.server.exception.E2EchoException;
import com.github.wrx886.e2echo.server.repository.MessageRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * 消息业务逻辑层。
 *
 * <p>对外统一以 {@link EccMessage} 作为视图，内部持久化为 {@link Message} 实体，
 * 封装消息的校验、保存与查询操作。</p>
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    /**
     * 消息数据访问对象。
     */
    private final MessageRepository messageRepository;

    /**
     * 保存消息。
     *
     * <p>先校验消息签名与发送时间，再转换为实体保存；实体时间戳取自消息 ID 前 16 位
     * 十六进制时间戳，便于后续按时间查询。</p>
     *
     * @param eccMessage 待保存的消息视图
     * @return 保存后的消息视图
     * @throws E2EchoException 消息校验失败、ID 格式错误或发送时间偏差过大
     */
    public EccMessage save(EccMessage eccMessage) {
        long timestamp = verify(eccMessage);
        Message message = toEntity(eccMessage);
        message.setTimestamp(timestamp);
        return toView(messageRepository.save(message));
    }

    /**
     * 根据主键查询消息。
     *
     * @param id 消息 ID
     * @return 消息视图
     * @throws E2EchoException 消息不存在
     */
    public EccMessage getById(String id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new E2EchoException("消息不存在：" + id));
        return toView(message);
    }

    /**
     * 按条件分页查询消息列表。
     *
     * <p>过滤条件均为可选，条件为空时不参与过滤。结果按消息 ID 排序：ID 前 16 位为定长的
     * 十六进制时间戳，其字典序与时间顺序一致，因此无需额外按时间戳排序即可保证时间先后。
     * 升序即从老到新，降序即从新到老。</p>
     *
     * @param fromList       发送者公钥列表，为空表示不过滤
     * @param toList         接收者信息列表，为空表示不过滤
     * @param channel        消息通道，为空表示不过滤
     * @param startTimestamp 起始时间戳（毫秒，含），为空表示不限制
     * @param endTimestamp   结束时间戳（毫秒，含），为空表示不限制
     * @param startId        起始消息 ID，仅返回 ID 大于该值的消息，为空表示不限制
     * @param order          排序方向，{@code asc} 从老到新（默认）、{@code desc} 从新到老
     * @param pageNum        页码，从 1 开始
     * @param pageSize       每页条数
     * @return 分页后的消息视图
     * @throws E2EchoException 时间戳格式错误、排序方向非法、页码或每页条数非法
     */
    public Page<EccMessage> list(
            List<String> fromList,
            List<String> toList,
            String channel,
            String startTimestamp,
            String endTimestamp,
            String startId,
            String order,
            int pageNum,
            int pageSize
    ) {
        if (pageNum < 1) {
            throw new E2EchoException("页码必须大于等于 1！");
        }
        if (pageSize < 1) {
            throw new E2EchoException("每页条数必须大于等于 1！");
        }

        Long start = parseTimestamp(startTimestamp);
        Long end = parseTimestamp(endTimestamp);
        Sort.Direction direction = parseOrder(order);

        Specification<Message> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (fromList != null && !fromList.isEmpty()) {
                predicates.add(root.get("from").in(fromList));
            }
            if (toList != null && !toList.isEmpty()) {
                predicates.add(root.get("to").in(toList));
            }
            if (channel != null && !channel.isBlank()) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Long>get("timestamp"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Long>get("timestamp"), end));
            }
            if (startId != null && !startId.isBlank()) {
                predicates.add(cb.greaterThan(root.<String>get("id"), startId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(direction, "id"));
        return messageRepository.findAll(specification, pageable).map(this::toView);
    }

    /**
     * 校验消息签名与发送时间。
     *
     * @param eccMessage 待校验的消息
     * @return 消息 ID 中携带的时间戳（毫秒）
     * @throws E2EchoException 签名校验失败、ID 格式错误或发送时间偏差过大
     */
    private long verify(EccMessage eccMessage) {
        // 校验签名
        if (!Ecc.verify(eccMessage)) {
            throw new E2EchoException("消息校验失败！");
        }

        // 时间：取自 id 前 16 位十六进制时间戳
        long timestamp;
        try {
            timestamp = Long.parseLong(eccMessage.getId().substring(0, 16), 16);
        } catch (Exception e) {
            throw new E2EchoException("消息 ID 格式错误！");
        }

        // 允许接收的时间范围
        if (Math.abs(System.currentTimeMillis() - timestamp) > 15 * 1000) {
            // 误差不大于 15s（客户端为 60s，客户端和服务端的时间差距不大于 5s）
            throw new E2EchoException("发送时间与服务器当前时间差距过大！");
        }

        return timestamp;
    }

    /**
     * 将十进制毫秒时间戳字符串解析为 {@link Long}。
     *
     * @param value 时间戳字符串，为空时返回 {@code null}
     * @return 解析后的时间戳，或 {@code null}
     * @throws E2EchoException 字符串不是合法的十进制数字
     */
    private Long parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new E2EchoException("时间戳格式错误：" + value);
        }
    }

    /**
     * 解析排序方向。
     *
     * @param order 排序方向，{@code asc} 表示按消息 ID 升序（从老到新）、{@code desc} 表示按
     *              消息 ID 降序（从新到老），为空时默认升序；取值不区分大小写
     * @return 排序方向
     * @throws E2EchoException 取值不是 asc 或 desc
     */
    private Sort.Direction parseOrder(String order) {
        if (order == null || order.isBlank()) {
            return Sort.Direction.ASC;
        }
        return switch (order.trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new E2EchoException("排序方向只能是 asc 或 desc：" + order);
        };
    }

    /**
     * 将消息视图转换为持久化实体。
     *
     * @param eccMessage 消息视图
     * @return 消息实体
     */
    private Message toEntity(EccMessage eccMessage) {
        Message message = new Message();
        BeanUtils.copyProperties(eccMessage, message);
        return message;
    }

    /**
     * 将持久化实体转换为消息视图。
     *
     * @param message 消息实体
     * @return 消息视图
     */
    private EccMessage toView(Message message) {
        EccMessage eccMessage = new EccMessage();
        BeanUtils.copyProperties(message, eccMessage);
        return eccMessage;
    }

}
