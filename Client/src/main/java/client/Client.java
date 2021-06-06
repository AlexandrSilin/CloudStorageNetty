package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class Client extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(
                new File("Client\\src\\main\\java\\client\\resources\\mainWindow.fxml").toURI().toURL());
        primaryStage.setTitle("Storage");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }
}
