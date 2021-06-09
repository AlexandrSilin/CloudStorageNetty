package client.handlers;

import client.Client;
import client.Controller;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class InputHandler extends ChannelInboundHandlerAdapter {
    private static Controller controller;
    private ByteBuf buf;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        buf = (ByteBuf) msg;
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
                in = new StringBuilder();
                while (buf.isReadable()) {
                    in.append((char) buf.readByte());
                }
                String[] alert = in.toString().split("%");
                Controller.alert(alert[0], alert[1]);
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
                controller = Client.getController();
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
        }
    }
}
