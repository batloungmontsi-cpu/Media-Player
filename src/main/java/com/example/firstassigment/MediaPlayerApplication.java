package com.example.firstassigment;

import com.example.firstassigment.controller.MediaPlayerController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MediaPlayerApplication extends Application {
    @Override
    public void start(Stage stage) {
        MediaPlayerController controller = new MediaPlayerController(stage);
        Scene scene = controller.createScene();

        stage.setTitle("Vintage Media Player");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setMaximized(false);
        stage.centerOnScreen();
        stage.show();

        // Keep the search box inactive until it is clicked; the global keyboard
        // shortcuts own the keyboard while no text field has focus.
        Platform.runLater(() -> scene.getRoot().requestFocus());
    }

    public static void main(String[] args) { launch(args); }
}