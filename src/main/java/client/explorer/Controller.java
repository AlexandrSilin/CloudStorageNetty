package client.explorer;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    TableView<FileInfo> table;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TableColumn<FileInfo, String> file = new TableColumn<>();
        file.setCellValueFactory(info -> new SimpleStringProperty(info.getValue().getType().getName()));
        file.setPrefWidth(30);
    }
}
