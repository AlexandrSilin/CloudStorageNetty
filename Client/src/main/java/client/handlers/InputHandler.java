package client.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

public class InputHandler extends ChannelInboundHandlerAdapter {
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
            filesList.append("List:");
            while (buf.isReadable()) {
                filesList.append((char) buf.readByte());
            }
            ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(filesList.toString().getBytes()));
        }
    }
}
