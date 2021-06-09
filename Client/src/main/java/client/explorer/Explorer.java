package client.explorer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class Explorer {

    /**
     * Отрисовка окна Explorer
     * @throws IOException
     */
    public Explorer() throws IOException {
        Stage exp = new Stage();
        Parent root = FXMLLoader.load(
                new File("Client/src/main/java/client/resources/explorer.fxml").toURI().toURL());
        exp.setTitle("Explorer");
        exp.setScene(new Scene(root, 800, 600));
        exp.show();
    }
}
