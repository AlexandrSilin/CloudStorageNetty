package server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;


public class OutputHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf)msg;
        StringBuilder out = new StringBuilder();
        while (buf.isReadable()) {
            char c = (char) buf.readByte();
            if (c == '%') {
                break;
            }
            out.append(c);
        }
        if (out.toString().startsWith("Message: ")) {
            ctx.channel().writeAndFlush(out.substring("Message: ".length()));
        } else {
            Path path = Path.of(out.toString());
            if (!Files.exists(path)){
                Files.createFile(path);
            }
            System.out.println(path);
            RandomAccessFile file = new RandomAccessFile(String.valueOf(path), "rw");
            while (buf.isReadable()) {
                file.write(buf.readByte());
            }
            System.out.println(buf);
            ctx.channel().writeAndFlush("Success");
        }
//        String[] message = String.valueOf(msg).trim().split(" ", 2);
//        ByteBuf buf = ctx.alloc().directBuffer();
//        StringBuilder out = new StringBuilder();
//        while (buf.isReadable()) {
//            out.append((char) buf.readByte());
//        }
//        System.out.println(out);
//        if (message[0].equals("Message:")) {
//            buf.writeBytes((message[1] + "\n").getBytes(StandardCharsets.UTF_8));
//            ctx.writeAndFlush(buf);
//        } else {
//            Path path = Path.of(message[1]);
//            ctx.flush();
//        }
    }

}
