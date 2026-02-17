package com.github.wrx886.e2echo.client.gui.content.chat;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.srv.AliasController;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatContentCell extends ListCell<Message> {

    private final AliasController aliasController = BeanProvider.getBean(AliasController.class);

    // 日期格式化工具
    private final SimpleDateFormat simpleDateFormat = BeanProvider.getBean(SimpleDateFormat.class);

    // 发送者
    private final Label fromNameLabel;

    // 发送时间
    private final Label sendTimeLabel;

    // 内容-文字
    private final Label dataTextLabel;

    // 展示容器
    private final VBox vBox;
    private final HBox hBox;

    // 构造函数
    public ChatContentCell() {

        // 发送者
        fromNameLabel = new Label();
        fromNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        fromNameLabel.setMaxWidth(330);

        // 发送时间
        sendTimeLabel = new Label();

        // 空白填充
        Region hBox1Region = new Region();
        HBox.setHgrow(hBox1Region, Priority.ALWAYS);

        // 发送者和发送时间容器
        hBox = new HBox(fromNameLabel, hBox1Region, sendTimeLabel);

        // 内容-文字
        dataTextLabel = new Label();
        dataTextLabel.setMaxWidth(430);
        dataTextLabel.setWrapText(true);

        // 容器
        vBox = new VBox(hBox, dataTextLabel);
    }

    @Override
    protected void updateItem(Message item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // 单元格为空
            setText(null);
            setGraphic(null);
        } else {
            // 单元格不为空

            // 设置文本高度最小值
            dataTextLabel.setMinHeight(0);

            // 发送者
            fromNameLabel.setText(aliasController.get(item.getFromPublicKeyHex()));

            // 发送时间
            Date date = new Date(item.getTimestamp());
            sendTimeLabel.setText(simpleDateFormat.format(date));

            if (MessageType.TEXT.equals(item.getType())) {
                // 文字展示
                dataTextLabel.setText(item.getData());
                dataTextLabel.setMinHeight(getTextHeight(dataTextLabel.getText()));
            } else {
                throw new UnsupportedOperationException("数据类型未处理！");
            }

            setGraphic(vBox);
        }
    }

    // 计算文本高度
    private int getTextHeight(String text) {
        Text text1 = new Text(text + "\n");
        text1.setFont(dataTextLabel.getFont());
        text1.setWrappingWidth(dataTextLabel.getMaxWidth());
        return (int) text1.getLayoutBounds().getHeight();
    }
}
