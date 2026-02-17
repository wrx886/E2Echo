package com.github.wrx886.e2echo.client.gui.store;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.gui.modol.vo.EditVo;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
@Component
public class GuiStore {

    // 会话列表
    private final ObservableList<Session> sessionList = FXCollections.observableArrayList();

    // 当前会话
    private final SimpleObjectProperty<Session> currentSession = new SimpleObjectProperty<>();

    // 当前会话消息
    private final ObservableList<Message> currentMessage = FXCollections.observableArrayList();

    // 当前会话名称
    private final SimpleStringProperty currentSessionName = new SimpleStringProperty();

    // 编辑的用户
    private final SimpleObjectProperty<EditVo> editVo = new SimpleObjectProperty<>();

    // 群聊列表
    private final ObservableList<String> groups = FXCollections.observableArrayList();

    // 当前群
    private final SimpleObjectProperty<String> currentGroup = new SimpleObjectProperty<>();

    // 当前群聊名称
    private final SimpleStringProperty currentGroupName = new SimpleStringProperty();

    // 当前群成员列表
    private final ObservableList<String> currentGroupMembers = FXCollections.observableArrayList();

}
