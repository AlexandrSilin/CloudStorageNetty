package client;

import client.explorer.Explorer;
import client.handlers.ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.Base64;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

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
        authWindow.close();
        Label label;
        if (login.getText().trim().length() == 0) {
            label = new Label("The field 'Login' must be filled");
        } else if (password.getText().trim().length() == 0) {
            label = new Label("The field 'Password' must be filled");
        } else {
            label = new Label("The field 'Password' must be filled");
            authButton.setDisable(true);
        }
        Stage answer = new Stage();
        answer.setTitle("Result");
        answer.setScene(new Scene(label, 100, 50));
        answer.show();
    }

    public void submitConnect(ActionEvent actionEvent) {
        serverAddress = ip.getText();
        Stage answer = new Stage();
        answer.setTitle("Result");
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
            answer.setScene(new Scene(new Label("Success"), 100, 50));
        } catch (NumberFormatException e) {
            answer.setScene(new Scene(new Label("Bad port"), 100, 50));
        } catch (Exception e) {
            answer.setScene(new Scene(new Label(e.getMessage()), 250, 50));
        }
        finally {
            answer.show();
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
