package com.github.wrx886.e2echo.client;

import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.service.AuthService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 客户端启动类。
 *
 * <p>以 Spring Boot 方式启动客户端，启动完成后生成一次性 auth 票据，并把带真实端口的登录地址
 * 输出到标准输出，便于开发调试时直接在浏览器中打开完成本地登录。</p>
 *
 * <p>正式使用时登录地址由 JavaFX 启动器获取并负责打开浏览器，这里输出到标准输出仅用于本地
 * 验证。</p>
 */
@SpringBootApplication
public class ClientApplication {

    /**
     * 客户端入口。
     *
     * <p>启动 Spring Boot 应用后，从 {@link Environment} 读取 Web 容器的实际端口
     * （{@code local.server.port}，服务端口配置为 0 时为随机端口），再通过 {@link BeanProvider}
     * 取得 {@link AuthService} 生成一次性 auth 票据，最后把
     * {@code http://localhost:{端口}/auth/{票据}} 形式的完整登录地址打印到标准输出。</p>
     *
     * @param args 命令行参数，原样交给 Spring Boot 处理
     */
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
        System.out.println( // 开发阶段使用，在没有GUI的情况下快速验证。
                "http://localhost:" +
                        BeanProvider.getBean(Environment.class).getProperty("local.server.port")
                        + "/auth/" +
                        BeanProvider.getBean(AuthService.class).newAndGetAuth());
    }

}
