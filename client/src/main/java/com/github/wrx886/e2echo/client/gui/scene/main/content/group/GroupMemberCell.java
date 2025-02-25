package com.github.wrx886.e2echo.client.gui.scene.main.content.group;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wrx886.e2echo.client.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.E2Echoxception;
import com.github.wrx886.e2echo.client.model.entity.GroupUser;
import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.service.GroupUserService;
import com.github.wrx886.e2echo.client.store.GuiStore;
import com.github.wrx886.e2echo.client.store.LoginUserStore;

import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class GroupMemberCell extends ListCell<User> {

    private GroupUserService groupUserService = BeanProvider.getBean(GroupUserService.class);

    private GuiStore guiStore = BeanProvider.getBean(GuiStore.class);

    private LoginUserStore loginUserStore = BeanProvider.getBean(LoginUserStore.class);

    // 该条目的用户信息
    private User member;

    // 名称
    private final Label memberNameLabel;

    // 公钥
    private final Label memberPublicKeyLabel;

    // 删除
    private final Button memberDeleteButton;

    // 容器
    private final VBox vBox;

    // 构造函数
    public GroupMemberCell() {
        // 名称
        memberNameLabel = new Label();

        // 公钥
        memberPublicKeyLabel = new Label();

        // 删除
        memberDeleteButton = new Button("删除");
        memberDeleteButton.setOnAction(this::memberDeleteButtonOnAction);

        // 容器
        vBox = new VBox(memberNameLabel, memberPublicKeyLabel, memberDeleteButton);
    }

    @Override
    protected void updateItem(User item, boolean empty) {
        super.updateItem(item, empty);

        // 设置该条目的用户信息
        this.member = item;

        if (item == null || empty) {
            setText(null);
            setGraphic(null);
        } else {
            memberNameLabel.setText(item.getName());
            memberPublicKeyLabel.setText(item.getPublicKey());

            setGraphic(vBox);
        }
    }

    // 删除用户信息
    private void memberDeleteButtonOnAction(Event event) {
        // 删除成员
        if (member != null) {
            // 不允许删除群主
            if (member.getPublicKey().equals(loginUserStore.getPublicKey())) {
                throw new E2Echoxception(null, "不允许删除群主");
            }

            groupUserService.remove(new LambdaQueryWrapper<GroupUser>()
                    .eq(GroupUser::getGroupId, loginUserStore.getId())
                    .eq(GroupUser::getGroupId, guiStore.getCurrentGroup().get().getId())
                    .eq(GroupUser::getMemberId, member.getId()));
            groupUserService.updateGroupMenbers(guiStore.getCurrentGroup().get().getId());
        }
    }
}
