package client;

import client.explorer.Explorer;
import client.handlers.ClientHandler;
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

public class Controller {
    private static Channel channel = null;

    Stage connectWindow;
    Stage authWindow;

    @FXML
    TextField login;

    @FXML
    PasswordField password;

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
        Parent auth = FXMLLoader.load(new File("Client\\src\\main\\java\\client\\resources\\auth.fxml").toURI().toURL());
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

    @FXML
    public void submitConnect(ActionEvent actionEvent) {
        serverAddress = ip.getText();
        Alert answer = new Alert(Alert.AlertType.INFORMATION, "Success", ButtonType.OK);
        try {
            serverPort = Integer.parseInt(port.getText());
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
                    .connect(serverAddress, serverPort)//)
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

    public void connect(ActionEvent actionEvent) throws IOException {
        connectWindow = new Stage();
        connectWindow.setTitle("Connect");
        Parent auth = FXMLLoader.load(new File("Client\\src\\main\\java\\client\\resources\\connect.fxml").toURI().toURL());
        connectWindow.setScene(new Scene(auth));
        connectWindow.show();
    }

    public static Channel getChannel() {
        return channel;
    }

    public void openExplorer(ActionEvent actionEvent) throws IOException {
        new Explorer();
    }

    public void help(ActionEvent actionEvent) {
        ByteBuf buf = Unpooled.wrappedBuffer("Command:--help".getBytes());
        channel.writeAndFlush(buf);
    }

    public void refreshList(ActionEvent actionEvent) {
        ByteBuf buf = Unpooled.wrappedBuffer("Command:ls".getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(buf);
    }
}
