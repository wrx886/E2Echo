package com.github.wrx886.e2echo.plugin.gui.common;

import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;

// 场景路由：用于切换舞台页面中的场景
// 建议：先注册全部场景，后使用切换功能
public final class SceneRouter {

    // 舞台
    @Getter
    private final Stage stage;

    // 存储场景类和场景的映射关系
    private final ConcurrentHashMap<Class<? extends Scene>, Scene> router = new ConcurrentHashMap<>();

    // 构造函数
    public SceneRouter(Stage stage) {
        this.stage = stage;
    }

    // 对外提供注册场景的方法
    public <E extends Scene> void register(E scene) {
        router.put(scene.getClass(), scene);
    }

    // 切换场景
    public void push(Class<? extends Scene> clazz) {
        Scene scene = router.get(clazz);
        if (scene == null) {
            throw new RuntimeException("Scene not registered!");
        }
        // 切换场景
        stage.setScene(scene);
    }

    // 获取场景
    @SuppressWarnings("unchecked")
    public <E extends Scene> E get(Class<E> clazz) {
        return (E) router.get(clazz);
    }

}
