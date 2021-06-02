package client;

import client.explorer.Explorer;
import client.handlers.ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class Controller {
    Stage connectWindow;
    Stage authWindow;

    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    TableView<String> filesTable;

    @FXML
    TextField ip;

    @FXML
    TextField port;

    @FXML
    Button connectButton;

    @FXML
    Button authButton;

    private String serverAddress;
    private int serverPort;

    public void auth(ActionEvent actionEvent) throws IOException {
        authWindow = new Stage();
        authWindow.setTitle("Authenticate");
        Parent auth = FXMLLoader.load(new File("src/main/java/client/resources/auth.fxml").toURI().toURL());
        authWindow.setScene(new Scene(auth));
        authWindow.show();
    }

    public void exit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void submitAuth(ActionEvent actionEvent) throws IOException {
        Alert answer = new Alert(Alert.AlertType.INFORMATION, "Success", ButtonType.OK);
        if (login.getText().trim().length() == 0) {
            answer.setContentText("The field 'Login' must be filled");
        } else if (password.getText().trim().length() == 0) {
            answer.setContentText("The field 'Password' must be filled");
        } else {
            authButton.setDisable(true);
        }
        answer.showAndWait();
    }

    public void submitConnect(ActionEvent actionEvent) {
        serverAddress = ip.getText();
        Alert answer = new Alert(Alert.AlertType.WARNING, "Success", ButtonType.OK);
        try {
            serverPort = Integer.parseInt(port.getText());
            EventLoopGroup client = new NioEventLoopGroup(1);
            Bootstrap connect = new Bootstrap();
            Channel channel = connect.group(client)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientHandler())
                    .connect(serverAddress, serverPort)
                    .sync()
                    .channel();
        } catch (NumberFormatException e) {
            answer.setContentText("Bad port");
        } catch (Exception e) {
            answer.setContentText(e.getMessage());
        } finally {
            answer.showAndWait();
        }
    }

    public void connect(ActionEvent actionEvent) throws IOException {
        connectWindow = new Stage();
        connectWindow.setTitle("Connect");
        Parent auth = FXMLLoader.load(new File("src/main/java/client/resources/connect.fxml").toURI().toURL());
        connectWindow.setScene(new Scene(auth));
        connectWindow.show();
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        new Explorer();
    }
}
