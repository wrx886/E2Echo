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
import com.github.wrx886.e2echo.ecc.util.IdUtil;
import com.github.wrx886.e2echo.server.entity.Message;
import com.github.wrx886.e2echo.server.exception.E2EchoException;
import com.github.wrx886.e2echo.server.repository.MessageRepository;
import com.github.wrx886.e2echo.server.vo.req.MessageListReqVo;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * 消息业务逻辑层。
 *
 * <p>对外统一以 {@link EccMessage} 作为视图，内部持久化为 {@link Message} 实体，
 * 封装消息的校验、保存与查询操作。</p>
 *
 * <p>消息保存成功后会通过 {@link NoticeService} 通知订阅了该消息接收者的客户端，使它们能够
 * 及时拉取新消息。</p>
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    /**
     * 页码上限：offset 越深查询越慢，更靠后的数据应该用游标翻页。
     */
    private static final int MAX_PAGE_NUM = 16;

    /**
     * 每页条数上限。
     */
    private static final int MAX_PAGE_SIZE = 256;

    /**
     * 消息数据访问对象。
     */
    private final MessageRepository messageRepository;

    /**
     * 通知业务逻辑对象，用于在消息保存成功后通知订阅者。
     */
    private final NoticeService noticeService;

    /**
     * 保存消息。
     *
     * <p>先校验消息的必填字段、签名与发送时间，再转换为实体保存；消息 ID 前 16 位已包含生成时间，
     * 无需额外保存时间字段。保存成功后，通知订阅了该消息接收者的客户端。</p>
     *
     * @param eccMessage 待保存的消息视图
     * @return 保存后的消息视图
     * @throws E2EchoException 必填字段为空、消息校验失败或发送时间偏差过大
     */
    public EccMessage save(EccMessage eccMessage) {
        verify(eccMessage);
        Message message = toEntity(eccMessage);
        EccMessage ret = toView(messageRepository.save(message));
        noticeService.notice(ret.getTo());
        return ret;
    }

    /**
     * 根据消息 ID 查询消息。
     *
     * @param id 消息 ID
     * @return 消息视图
     * @throws E2EchoException 消息不存在
     */
    public EccMessage getById(String id) {
        Message message = messageRepository.findByMessageId(id)
                .orElseThrow(() -> new E2EchoException("消息不存在：" + id));
        return toView(message);
    }

    /**
     * 按条件分页查询消息列表。
     *
     * <p>{@code channel} 必须提供，{@code from}、{@code to} 等其余过滤条件可选，条件为空时不参与
     * 过滤。结果按消息 ID 排序：ID 前 16 位为定长的
     * 十六进制时间戳，其字典序与时间顺序一致，因此无需额外按时间戳排序即可保证时间先后。
     * 升序即从老到新，降序即从新到老。消息 ID 已全局唯一，本身就能保证完整全序和翻页稳定性，
     * 无需再按主键排序。</p>
     *
     * <p>{@code startId}、{@code endId} 是分页游标，按排序方向选用：升序时把上一页最后一条的 ID
     * 作为 {@code startId}（只取更大的），降序时作为 {@code endId}（只取更小的），这样翻页不会
     * 取到已经取过的消息。</p>
     *
     * <p>{@code startTimestamp}、{@code endTimestamp} 仍按毫秒时间语义接收，服务层会将它们转换为
     * 消息 ID 的字典序上下界，从而继续使用 {@code message_id} 上的索引。</p>
     *
     * <p>分页深度有限制：页码不超过 {@value #MAX_PAGE_NUM}、每页条数不超过
     * {@value #MAX_PAGE_SIZE}，限制的是 offset 深翻页的开销，取更靠后的数据应该用游标。</p>
     *
     * @param reqVo 分页查询参数
     * @return 分页后的消息视图
     * @throws E2EchoException 通道为空、时间戳格式错误、排序方向非法、页码或每页条数超出范围
     */
    public Page<EccMessage> list(MessageListReqVo reqVo) {
        if (reqVo.channel() == null || reqVo.channel().isBlank()) {
            throw new E2EchoException("消息通道不能为空！");
        }

        // 页码与每页条数是必填项：缺省按 0 处理，与越界一样落到下面的范围校验
        int pageNum = reqVo.pageNum() == null ? 0 : reqVo.pageNum();
        int pageSize = reqVo.pageSize() == null ? 0 : reqVo.pageSize();

        if (pageNum < 1) {
            throw new E2EchoException("页码必须大于等于 1！");
        }
        if (pageNum > MAX_PAGE_NUM) {
            throw new E2EchoException("页码不能超过 " + MAX_PAGE_NUM + "！");
        }
        if (pageSize < 1) {
            throw new E2EchoException("每页条数必须大于等于 1！");
        }
        if (pageSize > MAX_PAGE_SIZE) {
            throw new E2EchoException("每页条数不能超过 " + MAX_PAGE_SIZE + "！");
        }

        String startIdFromTimestamp = parseTimestampPrefix(reqVo.startTimestamp(), 0);
        String endIdFromTimestamp = parseTimestampPrefix(reqVo.endTimestamp(), 1);
        Sort.Direction direction = parseOrder(reqVo.order());

        Specification<Message> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (reqVo.fromList() != null && !reqVo.fromList().isEmpty()) {
                predicates.add(root.get("from").in(reqVo.fromList()));
            }
            if (reqVo.toList() != null && !reqVo.toList().isEmpty()) {
                predicates.add(root.get("to").in(reqVo.toList()));
            }
            predicates.add(cb.equal(root.get("channel"), reqVo.channel()));
            if (startIdFromTimestamp != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("messageId"), startIdFromTimestamp));
            }
            if (endIdFromTimestamp != null) {
                predicates.add(cb.lessThan(root.get("messageId"), endIdFromTimestamp));
            }
            if (reqVo.startId() != null && !reqVo.startId().isBlank()) {
                predicates.add(cb.greaterThan(root.get("messageId"), reqVo.startId()));
            }
            if (reqVo.endId() != null && !reqVo.endId().isBlank()) {
                predicates.add(cb.lessThan(root.get("messageId"), reqVo.endId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 消息 ID 已全局唯一，单一排序键即可保证完整全序和稳定的分页边界
        Sort sort = Sort.by(direction, "messageId");
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        return messageRepository.findAll(specification, pageable).map(this::toView);
    }

    /**
     * 校验消息签名与发送时间。
     *
     * <p>先校验必填字段：这些字段在 {@code message} 表中都是非空列，提前校验可以避免把空值带到
     * 持久化层，也能给调用方一个明确的错误提示。</p>
     *
     * @param eccMessage 待校验的消息
     * @throws E2EchoException 必填字段为空、消息校验失败或发送时间偏差过大
     */
    private void verify(EccMessage eccMessage) {
        // 必填字段：与 message 表的非空约束保持一致
        requireText(eccMessage.getFrom(), "from");
        requireText(eccMessage.getTo(), "to");
        requireText(eccMessage.getMessage(), "message");
        requireText(eccMessage.getType(), "type");
        requireText(eccMessage.getChannel(), "channel");
        requireText(eccMessage.getInfo(), "info");

        // 校验签名
        if (!Ecc.verify(eccMessage)) {
            throw new E2EchoException("消息校验失败！");
        }

        // Ecc.verify 已校验 ID 字符串格式，这里只需兜底时间戳超出 long 范围的情况
        long timestamp;
        try {
            timestamp = IdUtil.timestampOf(eccMessage.getId());
        } catch (IllegalArgumentException e) {
            throw new E2EchoException("消息校验失败！");
        }

        // 允许接收的时间范围
        if (Math.abs(System.currentTimeMillis() - timestamp) > 15 * 1000) {
            // 误差不大于 15s（客户端为 60s，客户端和服务端的时间差距不大于 5s）
            throw new E2EchoException("发送时间与服务器当前时间差距过大！");
        }

    }

    /**
     * 校验消息的必填字段。
     *
     * <p>空白字符串与 {@code null} 一样都视为缺失：签名原文由字符串拼接而成，空白值对调用方没有
     * 意义，还会让按字段过滤、按字段分派消息的逻辑失去依据。</p>
     *
     * @param value 字段值
     * @param name  字段名，用于错误提示
     * @throws E2EchoException 字段为 {@code null} 或空白
     */
    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new E2EchoException("消息字段不完整：" + name + "！");
        }
    }

    /**
     * 将十进制毫秒时间戳字符串解析并转换为消息 ID 前缀。
     *
     * <p>起始时间直接取当前毫秒前缀作为包含下界；结束时间加一后取前缀作为开区间上界，
     * 从而覆盖结束毫秒内的所有消息 ID。</p>
     *
     * @param value           时间戳字符串，为空时返回 {@code null}
     * @param timestampOffset 时间戳偏移量，起始边界为 0，结束边界为 1
     * @return 消息 ID 前缀，或 {@code null}
     * @throws E2EchoException 字符串不是合法的十进制数字、时间戳为负数或偏移后超出范围
     */
    private String parseTimestampPrefix(String value, long timestampOffset) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long timestamp = Long.parseLong(value);
            if (timestamp < 0) {
                throw new E2EchoException("时间戳不能为负数：" + value);
            }
            return IdUtil.timestampPrefix(Math.addExact(timestamp, timestampOffset));
        } catch (NumberFormatException e) {
            throw new E2EchoException("时间戳格式错误：" + value);
        } catch (ArithmeticException e) {
            throw new E2EchoException("时间戳超出范围：" + value);
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
     * <p>{@code id} 在两边含义不同：视图里是消息 ID，实体里是数据库主键，因此不参与属性复制，
     * 消息 ID 显式写入 {@code messageId}，主键由基类生成。</p>
     *
     * @param eccMessage 消息视图
     * @return 消息实体
     */
    private Message toEntity(EccMessage eccMessage) {
        Message message = new Message();
        BeanUtils.copyProperties(eccMessage, message, "id");
        message.setMessageId(eccMessage.getId());
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
        BeanUtils.copyProperties(message, eccMessage, "id");
        eccMessage.setId(message.getMessageId());
        return eccMessage;
    }

}
