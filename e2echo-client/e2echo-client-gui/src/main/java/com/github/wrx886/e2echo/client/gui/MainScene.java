package com.github.wrx886.e2echo.client.gui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class MainScene extends Scene {

    public MainScene() {
        super(new Pane());

        Pane pane = (Pane) getRoot();

        pane.getChildren().add(new Label("Hello, World!"));

    }

}
