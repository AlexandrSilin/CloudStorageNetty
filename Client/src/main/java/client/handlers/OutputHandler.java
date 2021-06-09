package client.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            case "Command:":
                out = new StringBuilder();
                while (buf.isReadable()) {
                    c = (char) buf.readByte();
                    out.append(c);
                }
                ctx.writeAndFlush(buf.clear().writeBytes(out.toString().getBytes()));
                break;
            case "File:":
                out = new StringBuilder();
                while (buf.isReadable()) {
                    c = (char) buf.readByte();
                    out.append(c);
                }
                String toSend = "upload:";
                byte[] out1 = toSend.getBytes(StandardCharsets.UTF_8);
                byte[] out2 = Files.readAllBytes(Paths.get(out.toString()));
                byte[] allBytes = new byte[out1.length+out2.length];
                System.arraycopy(out1,0,allBytes,0,out1.length);
                System.arraycopy(out2,0,allBytes,out1.length,out2.length);
                ctx.writeAndFlush(allBytes);
                break;
        }
    }
}
