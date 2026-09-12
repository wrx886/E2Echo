package com.github.wrx886.e2echo.server.config;

import com.github.wrx886.e2echo.server.service.NoticeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 发布订阅配置。
 *
 * <p>为 {@link NoticeService} 注册订阅容器。每个实例只订阅自己的频道，即
 * {@code RedisPrefix.CHANNEL + 实例 ID}：当某个实例上的客户端订阅了目标时，其他实例发现该
 * 客户端后会把消息发布到它所属实例的频道，再由该实例把通知推送给自己的 SSE 连接。</p>
 */
@Configuration
public class PubSubConfig {

    /**
     * 创建并配置 Redis 消息监听容器。
     *
     * @param factory  Redis 连接工厂，由 Spring Boot 自动配置提供
     * @param listener 通知服务，同时作为消息监听器
     * @return 已订阅本实例频道的监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory factory,
                                                        NoticeService listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // 精确频道订阅：频道名必须完全一致
        container.addMessageListener(listener, new ChannelTopic(listener.getChannel()));

        return container;
    }

}
