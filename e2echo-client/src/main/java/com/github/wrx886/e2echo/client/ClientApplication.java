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
     * 按登入用户派生会话 cookie 名。
     *
     * <p>cookie 只按“域名 + 路径”保存、不区分端口，所以多个客户端实例都用默认名（{@code JSESSIONID}）
     * 时，浏览器只会存一份，后登入的实例会把前面实例的会话挤掉；而用随机名又会在浏览器里越积越多
     * （每次重启多一个）。这里改成按公钥派生：{@code E2ECHO_SESSION_<公钥前 8 位>} —— 同一个用户重启
     * 用同一个名字（不再累积），不同用户的名字不同（互不干扰）。</p>
     *
     * <p>必须在容器启动之前调用：会话 cookie 名是容器启动时读取的；未登入时保持
     * {@code application.yml} 里的配置不变。</p>
     */
    public static void configureSessionCookieName() {
        String publicKey = Ecc.getPublicKey();
        if (!StringUtils.hasLength(publicKey)) {
            return;
        }
        System.setProperty("server.servlet.session.cookie.name",
                "E2ECHO_SESSION_" + publicKey.substring(0, 8));
    }

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
            configureSessionCookieName();
            SpringApplication.run(ClientApplication.class, args);
        } else {
            Application.launch(MainApplication.class, args);
        }
    }

}
