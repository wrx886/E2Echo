package com.github.wrx886.e2echo.client.config;

import java.util.Optional;

import com.github.wrx886.e2echo.ecc.Ecc;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/**
 * 当前登入用户的提供者。
 *
 * <p>JPA 审计填充 {@code @CreatedBy} 时调用本类取审计人：登入用户的公钥保存在 {@link Ecc} 中，
 * 这里直接把公钥作为审计人，因此实体基类的 {@code owner} 字段记录的就是数据所属的用户。审计开关
 * 在启动类 {@code ClientApplication} 上（{@code @EnableJpaAuditing}）。</p>
 *
 * <p>每次填充都会重新读取 {@link Ecc#getPublicKey()}，所以切换登入用户（或登出后清空密钥对）
 * 之后的行为会立刻改变，不需要重启。</p>
 */
@Component
public class EccAuditorAware implements AuditorAware<String> {

    /**
     * 获取当前登入用户的公钥。
     *
     * @return 当前登入用户的公钥；尚未登入（未保存密钥对）时返回空
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(Ecc.getPublicKey());
    }
}
