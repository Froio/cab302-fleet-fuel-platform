package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class HelloApplication extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        DBManager.initialize();
        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("Fleet Fuel Platform");
        stage.setScene(scene);
        stage.show();
    }

    public static void switchScene(String fxml) {
        switchScene(fxml, Object.class, controller -> { });
    }

    public static <T> void switchScene(
            String fxml,
            Class<T> controllerType,
            Consumer<T> controllerInitializer
    ) {
        try {
            String authorizedView = ViewAccessPolicy.resolve(
                    fxml,
                    AppSession.getInstance().currentUser()
            );
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(authorizedView));
            Parent root = loader.load();
            if (authorizedView.equals(fxml)) {
                controllerInitializer.accept(controllerType.cast(loader.getController()));
            }
            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new Scene(root, 900, 600));
            } else {
                primaryStage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load view " + fxml, e);
        }
    }
}
