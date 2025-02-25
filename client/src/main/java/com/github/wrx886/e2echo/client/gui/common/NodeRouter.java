package com.github.wrx886.e2echo.client.gui.common;

import java.util.concurrent.ConcurrentHashMap;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

// 节点路由：用于切换节点（一次只显示一个节点）
// 建议：先注册全部节点，后使用切换功能
public final class NodeRouter {

    // 存储布局节点
    private final Pane pane;

    // 存储节点类和节点之间的映射关系
    private final ConcurrentHashMap<Class<? extends Node>, Node> router = new ConcurrentHashMap<>();

    // 上一个节点
    private Class<? extends Node> lastNode;

    // 构造函数
    public NodeRouter(Pane pane) {
        this.pane = pane;
    }

    // 注册
    public void register(Node node) {
        router.put(node.getClass(), node);
    }

    // 切换
    public void push(Class<? extends Node> clazz) {
        Node node = router.get(clazz);
        if (node == null) {
            throw new RuntimeException("节点 %s 未注册".formatted(clazz));
        }
        // 获取显示节点
        ObservableList<Node> children = pane.getChildren();
        if (!children.isEmpty()) {
            lastNode = children.get(0).getClass();
            if (children.get(0).getClass().equals(clazz)) {
                return;
            }
        }
        children.clear();
        children.add(node);
    }

    // 返回
    public void pop() {
        if (lastNode == null) {
            pane.getChildren().clear();
            return;
        }
        push(lastNode);
        lastNode = null;
    }

}
