package client.handlers;

import client.Client;
import client.Controller;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

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
        if (in.toString().equals("Message:")) {
            while (buf.isReadable()) {
                in.append((char) buf.readByte());
            }
            ctx.channel().writeAndFlush(buf.clear().writeBytes(in.toString().getBytes(StandardCharsets.UTF_8)));
        }
        if (in.toString().equals("List:")) {
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
                }
            }
        }
    }
}
