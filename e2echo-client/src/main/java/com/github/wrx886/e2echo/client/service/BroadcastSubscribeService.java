package com.github.wrx886.e2echo.client.service;

import com.github.wrx886.e2echo.client.dto.BroadcastSubscribeDto;
import com.github.wrx886.e2echo.client.entity.BroadcastSubscribe;
import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.repository.BroadcastSubscribeRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.wrx886.e2echo.client.util.CommonUtil.currentOwner;

/**
 * 广播订阅业务逻辑层。
 *
 * <p>维护当前用户的订阅与别名。订阅只存在本地库，服务端并不知道谁订阅了谁；按订阅拉取帖子在
 * {@code BroadcastService} 里。订阅按登入用户隔离。</p>
 */
@Service
@RequiredArgsConstructor
public class BroadcastSubscribeService {

    /**
     * 订阅数据访问对象。
     */
    private final BroadcastSubscribeRepository repository;

    /**
     * 查询当前用户对某个作者的订阅。
     *
     * @param publicKey 被订阅者的公钥
     * @return 订阅，没有订阅时返回 {@code null}
     */
    public BroadcastSubscribeDto getOne(String publicKey) {
        return Optional.ofNullable(repository.getByOwnerAndPublicKey(currentOwner(), publicKey))
                .map(BroadcastSubscribeDto::fromEntity).orElse(null);
    }

    /**
     * 分页列出当前用户的订阅，按主键倒序（新订阅的排在前面）。
     *
     * <p>关键词同时匹配被订阅者公钥与别名（不区分大小写），为空表示不过滤。</p>
     *
     * @param pageNum          页码，从 1 开始
     * @param pageSize         每页条数
     * @param publicKeyOrAlias 关键词，按被订阅者公钥或别名模糊匹配（不区分大小写）；为空表示不过滤
     * @return 订阅分页结果（DTO）
     */
    public Page<BroadcastSubscribeDto> list(int pageNum, int pageSize, String publicKeyOrAlias) {
        Specification<BroadcastSubscribe> specification = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // owner
            predicates.add(cb.equal(root.get("owner"), currentOwner()));

            // publicKeyOrAlias
            if (StringUtils.hasLength(publicKeyOrAlias)) {
                String keyword = "%" + publicKeyOrAlias.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("publicKey")), keyword),
                        cb.like(cb.lower(root.get("alias")), keyword)
                ));
            }

            return cb.and(predicates);
        };

        // 分页排序并返回
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "id"));
        return repository.findAll(specification, pageable).map(BroadcastSubscribeDto::fromEntity);
    }

    /**
     * 列出当前用户订阅的作者公钥，用于拉取订阅流时的 {@code from} 过滤。
     *
     * <p>没有订阅时返回空列表，而服务端把空的 {@code from} 当成不过滤，所以调用方不能直接把它交上去，
     * 要自己补一个占位。</p>
     *
     * @return 被订阅者的公钥列表，没有订阅时为空列表
     */
    public List<String> listSubscribedPublicKey() {
        return repository.findAllByOwner(currentOwner()).stream()
                .map(BroadcastSubscribe::getPublicKey).toList();
    }

    /**
     * 修改订阅别名：给已订阅的作者起一个本机展示用的名字，传空表示清除别名。
     *
     * @param publicKey 被订阅者的公钥
     * @param alias     新别名，可以为空（空表示清除）
     * @throws E2EchoException 还没有订阅这个作者
     */
    @Transactional
    public void setAlias(String publicKey, String alias) {
        BroadcastSubscribe subscribe = repository.getByOwnerAndPublicKey(currentOwner(), publicKey);
        if (subscribe == null) throw new E2EchoException("请先订阅！");
        subscribe.setAlias(alias);
        repository.save(subscribe);
    }

    /**
     * 订阅某个作者。
     *
     * @param publicKey 被订阅者的公钥
     * @throws E2EchoException 已经订阅过
     */
    @Transactional
    public void subscribe(String publicKey) {
        BroadcastSubscribe broadcastSubscribe = repository.getByOwnerAndPublicKey(currentOwner(), publicKey);
        if (broadcastSubscribe != null) throw new E2EchoException("不能重复订阅！");
        broadcastSubscribe = new BroadcastSubscribe();
        broadcastSubscribe.setPublicKey(publicKey);
        repository.save(broadcastSubscribe);
    }

    /**
     * 取消订阅。
     *
     * @param publicKey 被订阅者的公钥
     * @throws E2EchoException 没有订阅过这个作者
     */
    @Transactional
    public void unsubscribe(String publicKey) {
        BroadcastSubscribe broadcastSubscribe = repository.getByOwnerAndPublicKey(currentOwner(), publicKey);
        if (broadcastSubscribe == null) throw new E2EchoException("不能取消未订阅！");
        repository.delete(broadcastSubscribe);
    }

}
