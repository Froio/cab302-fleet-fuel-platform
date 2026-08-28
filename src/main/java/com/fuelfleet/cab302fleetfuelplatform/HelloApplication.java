package com.fuelfleet.cab302fleetfuelplatform;

import javafx.application.Application;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("Fleet Fuel Platform");
        stage.setScene(scene);
        stage.show();
    }

    public static void switchScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(HelloApplication.class.getResource(fxml));
            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new Scene(root, 900, 600));
            } else {
                primaryStage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
