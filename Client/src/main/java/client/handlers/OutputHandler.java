package client.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class OutputHandler extends ChannelOutboundHandlerAdapter {
    private ByteBuf buf;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        buf = (ByteBuf) msg;
        StringBuilder out = new StringBuilder();
        char c;
        while (buf.isReadable()) {
            c = (char) buf.readByte();
            out.append(c);
            if (c == ':') {
                break;
            }
        }
        switch (out.toString()) {
            case "Message:":
                while (buf.isReadable()) {
                    out.append((char) buf.readByte());
                }
                Alert answer = new Alert(Alert.AlertType.INFORMATION, out.toString(), ButtonType.OK);
                answer.showAndWait();
                break;
            case "Command:":
                out = new StringBuilder();
                while (buf.isReadable()) {
                    c = (char) buf.readByte();
                    out.append(c);
                }
                ctx.writeAndFlush(buf.clear().writeBytes(out.toString().getBytes()));
                break;
            case "File:":
                ctx.writeAndFlush(buf);
                break;
        }
    }
}
