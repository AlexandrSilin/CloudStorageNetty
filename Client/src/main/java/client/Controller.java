package client;

import client.explorer.Explorer;
import client.handlers.InputHandler;
import client.handlers.OutputHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    private static Channel channel = null;
    private static String nick = "";
    private static Path path = Path.of("root/" + nick);
    private static Path downloadPath;
    private static Path downloadFile;

    @FXML
    private TextField currentPath;
    @FXML
    private TextField login;
    @FXML
    private PasswordField password;
    @FXML
    private TextField folderName;
    @FXML
    private TableView<FileOnServer> filesTable;
    @FXML
    private TextField ip;
    @FXML
    private TextField port;
    @FXML
    private TableColumn<FileOnServer, String> fileType;
    @FXML
    private TableColumn<FileOnServer, String> fileName;
    @FXML
    private TableColumn<FileOnServer, String> lastModified;

    private final List<FileOnServer> fileOnServerList = new ArrayList<>();
    private boolean initTableCols;

    public static Channel getChannel() {
        return channel;
    }

    public static void setNick(String nick) {
        Controller.nick = nick;
        path = Path.of("root/" + nick);
    }

    /**
     * Вывод сообщения
     * @param message String message
     * @param type String type of alert
     */
    public static void alert(String message, String type) {
        Alert alert;
        switch (type) {
            case "error":
                alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
                break;
            case "warning":
                alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
                break;
            default:
                alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        }
        alert.setTitle("Message from server");
        alert.showAndWait();
    }

    public static Path getDownloadFile() {
        return downloadFile;
    }

    public static Path getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String path) {
        downloadPath = Path.of(path);
    }

    /**
     * Отрисовка окна авторизации
     * @throws IOException
     */
    public void auth() throws IOException {
        Stage authWindow = new Stage();
        authWindow.setTitle("Authenticate");
        Parent auth = FXMLLoader.load(new File("Client/src/main/java/client/resources/auth.fxml").toURI().toURL());
        authWindow.setScene(new Scene(auth));
        authWindow.show();
    }

    /**
     * Авторизация на сервере
     * @param actionEvent ActionEvent
     */
    public void submitAuth(ActionEvent actionEvent) {
        Alert answer = new Alert(Alert.AlertType.INFORMATION, "Success", ButtonType.OK);
        String login = this.login.getText().trim();
        String password = this.password.getText().trim();
        ((Stage) (((Button) actionEvent.getSource()).getScene().getWindow())).close();
        if (login.length() == 0) {
            answer.setContentText("The field 'Login' must be filled");
            answer.showAndWait();
        } else if (password.length() == 0) {
            answer.setContentText("The field 'Password' must be filled");
            answer.showAndWait();
        } else {
            channel.writeAndFlush(Unpooled.wrappedBuffer(("Command:auth " + login + " " + password)
                    .getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * Завершение работы
     */
    public void exit() {
        if (channel != null) {
            channel.writeAndFlush(Unpooled.wrappedBuffer("Command:exit".getBytes(StandardCharsets.UTF_8)));
        }
        Platform.exit();
    }

    /**
     * Подключение к серверу
     * @param actionEvent ActionEvent
     */
    @FXML
    public void submitConnect(ActionEvent actionEvent) {
        initTableCols = false;
        String serverAddress = ip.getText();
        Alert answer = new Alert(Alert.AlertType.INFORMATION, "Success", ButtonType.OK);
        try {
            int serverPort = Integer.parseInt(port.getText());
            EventLoopGroup client = new NioEventLoopGroup(1);
            Bootstrap connect = new Bootstrap();
            channel = connect.group(client)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(
                                    new InputHandler(),
                                    new OutputHandler()
                            );
                        }
                    })
                    .connect(serverAddress, serverPort)
                    .sync()
                    .channel();
        } catch (NumberFormatException e) {
            answer.setContentText("Bad port");
        } catch (Exception e) {
            answer.setContentText(e.getMessage());
        } finally {
            answer.showAndWait();
            ((Stage) (((Button) actionEvent.getSource()).getScene().getWindow())).close();
        }
    }

    /**
     * Отрисовка формы для подключения
     * @throws IOException
     */
    public void connect() throws IOException {
        Stage connectWindow = new Stage();
        connectWindow.setTitle("Connect");
        FXMLLoader loader = new FXMLLoader(new File("Client/src/main/java/client/resources/connect.fxml").toURI().toURL());
        Parent auth = loader.load();
        connectWindow.setScene(new Scene(auth));
        connectWindow.show();
    }

    /**
     * Отрисовка формы для работы с файловым менеджером
     * @throws IOException
     */
    public void upload() throws IOException {
        new Explorer();
    }

    /**
     * Загрузка файлов с сервера
     * @throws IOException
     */
    public void download() throws IOException {
        if (filesTable.getSelectionModel().getSelectedItem() != null) {
            String filename = filesTable.getSelectionModel().getSelectedItem().getFilename();
            downloadPath = Path.of(currentPath.getText());
            downloadFile = Path.of(downloadPath + "/" + filename);
            if (Files.isDirectory(downloadFile)) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "It's a directory", ButtonType.OK);
                alert.setTitle("Warning");
                alert.showAndWait();
            } else {
                new Explorer();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Choose file before", ButtonType.OK);
            alert.setTitle("Warning");
            alert.showAndWait();
        }
    }

    public void help() {
        ByteBuf buf = Unpooled.wrappedBuffer("Command:--help".getBytes());
        channel.writeAndFlush(buf);
    }

    /**
     * Обновление файлов в таблице содержащей список файлов хранящихся на сервере
     * @param actionEvent ActionEvent
     */
    public void refreshList(ActionEvent actionEvent) {
        ByteBuf buf = Unpooled.wrappedBuffer("Command:ls".getBytes(StandardCharsets.UTF_8));
        if (!initTableCols) {
            TableColumn<FileOnServer, String> fileNameColumn = fileName;
            fileNameColumn.setCellValueFactory(info -> new SimpleStringProperty(info.getValue().getFilename()));
            TableColumn<FileOnServer, String> fileTypeColumn = fileType;
            fileTypeColumn.setCellValueFactory(info -> new SimpleStringProperty(info.getValue().getType()));
            TableColumn<FileOnServer, String> lastModifiedColumn = lastModified;
            lastModifiedColumn.setCellValueFactory(info -> new SimpleStringProperty(info.getValue().getLastModified()));
            filesTable.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() == 2) {
                    Path path = Path.of(currentPath.getText())
                            .resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(path)) {
                        Controller.path = path;
                        try {
                            goToDirectory(actionEvent, filesTable.getSelectionModel().getSelectedItem().getFilename());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            initTableCols = true;
        }
        currentPath.setText(String.valueOf(path));
        fileOnServerList.clear();
        channel.writeAndFlush(buf);
    }

    /**
     * Пополнение таблицы для отображения файлов
     * @param items String[] item
     */
    public void addItemsToList(String[] items) {
        filesTable.getItems().clear();
        if (items.length > 0) {
            fileOnServerList.add(new FileOnServer(items[0], items[1], items[2]));
            filesTable.getItems().addAll(fileOnServerList);
        }
    }

    /**
     * Переход на уровень выше в файловом менеджере сервера
     * @param actionEvent ActionEvent
     */
    public void up(ActionEvent actionEvent) {
        if (path.equals(Path.of("root/" + nick))) {
            return;
        }
        path = path.getParent();
        channel.writeAndFlush(Unpooled.wrappedBuffer("Command:cd ..".getBytes(StandardCharsets.UTF_8)));
        forceRefreshTable(actionEvent);
    }

    /**
     * Переход в папку в файловом менеджере сервера
     * @param actionEvent ActionEvent
     * @param catalog String to catalog
     * @throws InterruptedException
     */
    public void goToDirectory(ActionEvent actionEvent, String catalog) throws InterruptedException {
        currentPath.setText(String.valueOf(path));
        channel.writeAndFlush(Unpooled.wrappedBuffer(("Command:cd " + catalog).getBytes(StandardCharsets.UTF_8)));
        forceRefreshTable(actionEvent);
    }

    /**
     * Отрисовка окна для создания папки на стороне сервера
     * @throws IOException
     */
    public void createFolder() throws IOException {
        Stage dialog = new Stage();
        dialog.setTitle("Create Folder");
        FXMLLoader loader = new FXMLLoader(new File("Client/src/main/java/client/resources/dialog.fxml").toURI().toURL());
        Parent auth = loader.load();
        dialog.setScene(new Scene(auth));
        dialog.show();
    }

    /**
     * Создание папки на стороне сервера
     * @param actionEvent ActionEvent
     */
    public void submitCreateFolder(ActionEvent actionEvent) {
        String folderName = this.folderName.getText().trim();
        ((Stage) (((Button) actionEvent.getSource()).getScene().getWindow())).close();
        channel.writeAndFlush(Unpooled.wrappedBuffer(("Command:mkdir " + folderName).getBytes(StandardCharsets.UTF_8)));
        forceRefreshTable(actionEvent);
    }

    /**
     * Удаление файла на сервере
     * @param actionEvent ActionEvent
     */
    public void deleteFile(ActionEvent actionEvent) {
        String filename = filesTable.getSelectionModel().getSelectedItem().getFilename();
        if (filename.length() > 0) {
            channel.writeAndFlush(Unpooled.wrappedBuffer(("Command:rm " + filename).getBytes(StandardCharsets.UTF_8)));
            forceRefreshTable(actionEvent);
        }
    }

    /**
     * Обновление файлового менеджера
     * @param actionEvent ActionEvent
     */
    private void forceRefreshTable(ActionEvent actionEvent) {
        synchronized (this) {
            try {
                this.wait(200);
                refreshList(actionEvent);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
