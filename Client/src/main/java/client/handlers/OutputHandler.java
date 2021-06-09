package client.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutputHandler extends ChannelOutboundHandlerAdapter {

    /**
     * Обработчик исходящих сообщений
     * @param ctx ChannelHandlerContext
     * @param msg Object
     * @param promise ChannelPromise
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ByteBuf buf = (ByteBuf) msg;
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
                ctx.writeAndFlush(buf);
                break;
        }
    }
}
