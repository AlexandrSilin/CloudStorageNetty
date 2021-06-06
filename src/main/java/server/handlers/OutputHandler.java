package server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class OutputHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf)msg;
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
