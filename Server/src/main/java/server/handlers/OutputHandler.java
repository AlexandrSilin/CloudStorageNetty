package server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.nio.charset.StandardCharsets;


public class OutputHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        StringBuilder out = new StringBuilder();
        while (buf.isReadable()) {
            char c = (char) buf.readByte();
            if (c == ':') {
                break;
            }
            out.append(c);
        }
        if (out.toString().startsWith("Message: ")) {
            buf.clear().writeBytes(out.append('\n').toString().getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(buf);
        } else {
            ctx.writeAndFlush(buf);
        }
    }
}
