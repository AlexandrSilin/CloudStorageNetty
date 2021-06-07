package client;

import client.handlers.InputHandler;
import client.handlers.OutputHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class Connect {
    private static Channel channel = null;
    Stage connectWindow;
    @FXML
    TextField ip;
    @FXML
    TextField port;
    @FXML
    Button connectButton;
    private String serverAddress;
    private int serverPort;

    public Connect() throws IOException {
        connectWindow = new Stage();
        connectWindow.setTitle("Connect");
        FXMLLoader loader = new FXMLLoader(new File("Client/src/main/java/client/resources/connect.fxml").toURI().toURL());
        Parent auth = loader.load();
        connectWindow.setScene(new Scene(auth));
        connectWindow.show();
    }

    public static Channel getChannel() {
        return channel;
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
                    .connect(serverAddress, serverPort)
                    .sync()
                    .channel();


//            fileType.setCellValueFactory(new PropertyValueFactory<>("fileType"));
//            fileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
//            lastModified.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        } catch (NumberFormatException e) {
            answer.setContentText("Bad port");
        } catch (Exception e) {
            answer.setContentText(e.getMessage());
        } finally {
            answer.showAndWait();
            ((Stage) (((Button) actionEvent.getSource()).getScene().getWindow())).close();
        }
    }
}
