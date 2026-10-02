package com.github.wrx886.e2echo.client.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 容器关闭时的清理任务收集器。
 *
 * <p>需要“无论如何退出都要执行”的收尾逻辑（例如释放登入占用、结束前端 SSE 连接）注册到这里，容器
 * 关闭时统一执行。放在这里而不是 JavaFX 的退出回调里，是因为命令行关闭（Ctrl+C、kill）只会走 Spring
 * 的关闭流程、不会触发 JavaFX 的回调；反之界面退出最终也会关闭容器，所以注册一次就能覆盖所有退出
 * 路径。</p>
 */
@Slf4j
@Component
public class ContextClosedEventHandler {

    /**
     * 待执行的清理任务：键是任务本身（按对象身份区分），值用 {@code Optional} 占位，因为
     * {@code ConcurrentHashMap} 不允许存 {@code null}。
     */
    private final ConcurrentHashMap<Runnable, Optional<Void>> cleanTask = new ConcurrentHashMap<>();

    /**
     * 容器关闭时依次执行所有清理任务。
     *
     * <p>单个任务失败只记日志，不影响其余任务：清理本来就发生在退出阶段，一个收尾动作失败不应该
     * 让别的收尾动作也跟着漏掉（例如登入占用的释放）。</p>
     */
    @EventListener(ContextClosedEvent.class)
    public void cleanUp() {
        cleanTask.forEach((runnable, unused) -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Clean task failed.", e);
            }
        });
    }

    /**
     * 注册一个清理任务。
     *
     * @param runnable 容器关闭时要执行的任务
     */
    public void addCleanTask(Runnable runnable) {
        cleanTask.put(runnable, Optional.empty());
    }

}
