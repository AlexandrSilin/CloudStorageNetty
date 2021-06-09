package client.explorer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private static Path downloadPath = null;

    @FXML
    TableView<FileInfo> table;

    @FXML
    ComboBox<String> disks;

    @FXML
    TextField pathField;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>("Type");
        fileTypeColumn.setCellValueFactory(info -> new SimpleStringProperty(info.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(100);
        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Filename");
        fileNameColumn.setCellValueFactory(info -> new SimpleStringProperty(info.getValue().getFilename()));
        fileNameColumn.setPrefWidth(300);
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Last changes");
        fileDateColumn.setCellValueFactory(info -> new SimpleStringProperty(info.getValue().getLastModified()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        fileDateColumn.setPrefWidth(400);
        table.getColumns().addAll(fileTypeColumn, fileNameColumn, fileDateColumn);
        disks.getItems().clear();
        FileSystems.getDefault().getRootDirectories().forEach(item -> disks.getItems().add(String.valueOf(item)));
        disks.getSelectionModel().select(0);
        table.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                Path path = Path.of(pathField.getText()).resolve(table.getSelectionModel().getSelectedItem().getFilename());
                if (Files.isDirectory(path)) {
                    updateList(path);
                }
            }
        });
        updateList(Paths.get(disks.getValue()));
    }

    public void updateList(Path path) {
        try {
            pathField.setText(String.valueOf(path));
            table.getItems().clear();
            table.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            table.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK);
            alert.setTitle("Message");
            alert.showAndWait();
        }
    }

    public void pathUp(ActionEvent actionEvent) {
        Path path = Paths.get(pathField.getText()).getParent();
        if (path != null) {
            updateList(path);
        }
    }

    public void selectDisk(ActionEvent actionEvent) {
        updateList(Paths.get(disks.getValue()));
    }

    public void upload(ActionEvent actionEvent) {
        ((Stage) (((Button) actionEvent.getSource()).getScene().getWindow())).close();
        Path path = Path.of(pathField.getText()).resolve(table.getSelectionModel().getSelectedItem().getFilename());
        String toSend = path + "%";
        client.Controller.getChannel().writeAndFlush(Unpooled.wrappedBuffer(toSend.getBytes(StandardCharsets.UTF_8)));
    }

    public void download(ActionEvent actionEvent) {
        Path path = Path.of(pathField.getText()).resolve(table.getSelectionModel().getSelectedItem().getFilename());
        if (Files.isDirectory(path)) {
            downloadPath = path;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "It's a file!", ButtonType.OK);
            alert.showAndWait();
        }
    }
}
