package server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.nio.charset.StandardCharsets;

public class OutputHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        String[] message = String.valueOf(msg).trim().split(" ",2);
        ByteBuf buf = ctx.alloc().directBuffer();
        if (message[0].equals("Message:")) {
            buf.writeBytes((message[1] + "\n").getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(buf);
        } else {
            buf.writeBytes(message[1].getBytes());
        }
    }
}
