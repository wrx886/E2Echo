package com.github.wrx886.e2echo.client;

import com.github.wrx886.e2echo.client.exception.E2EchoException;
import com.github.wrx886.e2echo.client.fx.MainApplication;
import com.github.wrx886.e2echo.ecc.Ecc;
import javafx.application.Application;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

/**
 * 客户端启动类。
 *
 * <p>一个进程只能有一套 JavaFX 会话（{@code Application.launch} 只能调用一次，工具箱退出后也无法
 * 重新启动），因此这里只启动唯一的界面程序 {@link MainApplication}，由它负责登入、启动本容器、
 * 打开主界面的完整流程。</p>
 *
 * <p>这里用 {@link EnableJpaAuditing} 开启 JPA 审计，实体基类上的 {@code @CreatedBy}、
 * {@code @CreatedDate}、{@code @LastModifiedDate} 因此生效：审计人由
 * {@link com.github.wrx886.e2echo.client.config.EccAuditorAware} 提供，取的是当前登入用户的公钥，
 * 填充到实体的 {@code owner} 字段。没有这个注解时，实体上虽然标了这些注解也不会自动填值。</p>
 */
@EnableJpaAuditing
@SpringBootApplication
@EnableTransactionManagement
public class ClientApplication {

    @Getter
    @Setter
    private static boolean skipLogin = false;

    /**
     * 客户端入口。
     *
     * @param args 命令行参数，原样交给 JavaFX 与后续的 Spring Boot
     */
    public static void main(String[] args) {
        if (skipLogin) {
            if (!StringUtils.hasLength(Ecc.getPublicKey())) {
                throw new E2EchoException("未登入！");
            }
            SpringApplication.run(ClientApplication.class, args);
        } else {
            Application.launch(MainApplication.class, args);
        }
    }

}
