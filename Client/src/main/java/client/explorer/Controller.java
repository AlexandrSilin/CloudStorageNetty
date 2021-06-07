package client.explorer;

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
        try {
            Path path = Path.of(pathField.getText()).resolve(table.getSelectionModel().getSelectedItem().getFilename());
            if (!Files.isDirectory(path)) {
                File file = new File(String.valueOf(path));
                RandomAccessFile src = new RandomAccessFile(file, "rw");
                int offset = 0;
                int part = 512;
                int toSend = 0;
                int i;
                String command = "File:upload " + file.getName() + "%";
                byte[] send = new byte[part + command.length()];
                byte[] bufCommand = new byte[command.length()];
                for (int j = 0; j < command.length(); j++) {
                    bufCommand[j] = (byte) command.charAt(j);
                }
                while (offset != src.length()) {
                    if (src.length() < part) {
                        send = new byte[(int) src.length()];
                        while ((i = src.read()) != -1) {
                            send[offset] = (byte) i;
                            offset++;
                        }
                        byte[] full = new byte[send.length + bufCommand.length];
                        System.arraycopy(("File:upload " + file.getName() + "%")
                                .getBytes(StandardCharsets.UTF_8), 0, full, 0, bufCommand.length);
                        System.arraycopy(send, bufCommand.length, full, bufCommand.length, send.length);
                        client.Controller.getChannel().writeAndFlush(full);
                    } else {
                        while (offset != src.length()) {
                            send[toSend] = (byte) src.read();
                            offset++;
                            toSend++;
                            if (toSend == part) {
                                toSend = 0;
                                byte[] full = new byte[send.length + bufCommand.length];
                                System.arraycopy(("File:upload " + file.getName() + "%")
                                        .getBytes(StandardCharsets.UTF_8), 0, full, 0, bufCommand.length);
                                System.arraycopy(send, 0, full, bufCommand.length, send.length);
                                send = new byte[part + bufCommand.length];
                                client.Controller.getChannel().writeAndFlush(Unpooled.wrappedBuffer(full));
                            }
                        }
                    }
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "It's a directory!", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
