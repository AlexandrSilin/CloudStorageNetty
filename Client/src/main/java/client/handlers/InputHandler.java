package client.handlers;

import client.Client;
import client.Controller;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class InputHandler extends ChannelInboundHandlerAdapter {

    /**
     * Обработчик входящих сообщений
     * @param ctx ChannelHandlerContext
     * @param msg Object received message
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        StringBuilder in = new StringBuilder();
        char c;
        while (buf.isReadable()) {
            c = (char) buf.readByte();
            in.append(c);
            if (c == ':') {
                break;
            }
        }
        switch (in.toString()) {
            case "Message:":
                while (buf.isReadable()) {
                    in.append((char)buf.readByte());
                }
                ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(in.toString().getBytes(StandardCharsets.UTF_8)));
                break;
            case "nick:":
                in = new StringBuilder();
                while (buf.isReadable()) {
                    in.append((char) buf.readByte());
                }
                Controller.setNick(in.toString());
                break;
            case "List:":
                StringBuilder filesList = new StringBuilder();
                Controller controller = Client.getController();
                while (buf.isReadable()) {
                    char ch = (char) buf.readByte();
                    if (ch == '%') {
                        String[] file = filesList.toString().split(" ");
                        controller.addItemsToList(file);
                        filesList = new StringBuilder();
                    } else {
                        filesList.append(ch);
                        if (filesList.toString().equals("!!")) {
                            controller.addItemsToList(new String[0]);
                        }
                    }
                }
                break;
            case "File:":
                try {
                    StringBuilder filename = new StringBuilder();
                    while (buf.isReadable()) {
                        c = (char) buf.readByte();
                        if (c == '%') {
                            break;
                        }
                        filename.append(c);
                    }
                    Path path = Path.of(client.Controller.getDownloadPath() + "/" + filename);
                    File f = new File(String.valueOf(path));
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                        Client.getController().forceRefreshTable(new ActionEvent());
                    }
                    RandomAccessFile file = new RandomAccessFile(f, "rw");
                    while (buf.isReadable()) {
                        file.write(buf.readByte());
                    }
                    ctx.writeAndFlush(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
