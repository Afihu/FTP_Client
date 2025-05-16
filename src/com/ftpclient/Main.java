package com.ftpclient;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.ftpclient.gui.MainController; // This will need to be rewritten for JavaFX

public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
    }
    
    public static void main(String[] args) {
        launch(args); // This replaces SwingUtilities.invokeLater
    }
}