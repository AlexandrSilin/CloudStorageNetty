package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class Client extends Application {
    private static Controller controller;

    public static void main(String[] args) {
        launch(args);
    }

    public static Controller getController() {
        return controller;
    }

    /**
     * Отрисовка главного окна
     * @param primaryStage Stage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(new File("Client/src/main/java/client/resources/mainWindow.fxml").toURI().toURL());
        Parent root = loader.load();
        controller = loader.getController();
        primaryStage.setTitle("Storage");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }
}
