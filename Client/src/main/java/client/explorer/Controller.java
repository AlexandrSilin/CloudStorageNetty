package client.explorer;

import client.Client;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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

    @FXML
    private TableView<FileInfo> table;
    @FXML
    private ComboBox<String> disks;
    @FXML
    private TextField pathField;

    /**
     * Инициализация таблицы
     * @param url URL
     * @param resourceBundle ResourceBundle
     */
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

    /**
     * Обновление таблицы
     * @param path Path to file
     */
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

    /**
     * Переход на уровень выше в файловой системе
     */
    public void pathUp() {
        Path path = Paths.get(pathField.getText()).getParent();
        if (path != null) {
            updateList(path);
        }
    }


    /**
     * Выбор диска файловой системы
     */
    public void selectDisk() {
        updateList(Paths.get(disks.getValue()));
    }

    /**
     * Отправка файла на сервер
     * @param actionEvent ActionEvent
     */
    public void upload(ActionEvent actionEvent) {
        ((Stage) (((Button) actionEvent.getSource()).getScene().getWindow())).close();
        try {
            Path path = Path.of(pathField.getText()).resolve(table.getSelectionModel().getSelectedItem().getFilename());
            if (!Files.isDirectory(path)) {
                File file = new File(String.valueOf(path));
                RandomAccessFile src = new RandomAccessFile(file, "r");
                String command = "File:upload " + file.getName() + "%";
                if (file.length() > Integer.MAX_VALUE) {
                    byte[] fileBytes = new byte[(int) file.length()];
                    src.readFully(fileBytes);
                    byte[] bufCommand = new byte[command.length()];
                    for (int j = 0; j < command.length(); j++) {
                        bufCommand[j] = (byte) command.charAt(j);
                    }
                    ByteBuf buf = Unpooled.copiedBuffer(bufCommand, fileBytes);
                    client.Controller.getChannel().writeAndFlush(buf);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Too big file", ButtonType.OK);
                    alert.showAndWait();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "It's a directory!", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получение файла с сервера
     */
    public void download() {
        Path path = client.Controller.getDownloadFile();
        if (!Files.isDirectory(path)) {
            Client.getController().setDownloadPath(pathField.getText());
            client.Controller.getChannel()
                    .writeAndFlush(Unpooled.wrappedBuffer(("Command:download " + path).getBytes(StandardCharsets.UTF_8)));
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "It's a file!", ButtonType.OK);
            alert.showAndWait();
        }
    }
}
