package com.github.wrx886.e2echo.client.store;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.model.vo.MessageVo;
import com.github.wrx886.e2echo.client.model.vo.SessionVo;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

// GUI 界面显示所需容器
@Component
@Getter
public class GuiStore {

    // 会话列表
    private final ObservableList<SessionVo> sessionVos = FXCollections.observableArrayList();

    // 当前会话名称
    private final SimpleStringProperty currentSessionName = new SimpleStringProperty();

    // 当前会话消息
    private final ObservableList<MessageVo> currentMessageVos = FXCollections.observableArrayList();

    // 当前会话
    private final SimpleObjectProperty<SessionVo> currentSessionVo = new SimpleObjectProperty<>();

    // 编辑的用户
    private final SimpleObjectProperty<User> editUser = new SimpleObjectProperty<>();

    // 群聊列表
    private final ObservableList<User> groups = FXCollections.observableArrayList();

    // 当前群
    private final SimpleObjectProperty<User> currentGroup = new SimpleObjectProperty<>();

    // 当前群聊名称
    private final SimpleStringProperty currentGroupName = new SimpleStringProperty();

    // 当前群成员列表
    private final ObservableList<User> currentGroupMembers = FXCollections.observableArrayList();

}
