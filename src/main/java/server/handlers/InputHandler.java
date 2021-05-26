package server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.stream.ChunkedNioFile;
import server.Connect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Objects;

public class InputHandler extends ChannelInboundHandlerAdapter {
    public static final String LS_COMMAND = "\tls view all files and directories\n";
    public static final String MKDIR_COMMAND = "\tmkdir [dirname] create directory\n";
    public static final String CD_COMMAND = "\tcd go to directory\n";
    public static final String RM_COMMAND = "\trm [filename] delete file\n";
    public static final String NICKNAME_COMMAND = "\tnickname show your nickname\n";
    public static final String UPLOAD = "\tupload [path] [filename] upload your file in current directory on server\n";

    private Connection connection = null;
    private Path path = Path.of("root");
    private String nick;
    private boolean isAuth = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected " + ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws SQLException {
        ByteBuf buf = (ByteBuf) msg;
        StringBuilder builder = new StringBuilder();
        while (buf.isReadable()) {
            builder.append((char) buf.readByte());
        }
        String[] command = builder.toString().trim().split(" ", 2);
        if (command[0].equals("auth")) {
            auth(ctx, command[1].split(" "));
        }
        if (isAuth) {
            switch (command[0]) {
                case "--help":
                    ctx.channel().writeAndFlush("Message: \n" + LS_COMMAND + MKDIR_COMMAND + CD_COMMAND + RM_COMMAND +
                            UPLOAD + NICKNAME_COMMAND);
                    break;
                case "upload":
                    if (command.length < 2) {
                        ctx.channel().writeAndFlush("Message: Bad command");
                    } else {
                        uploading(ctx, command[1]);
                    }
                    break;
                case "ls":
                    ctx.channel().writeAndFlush("Message: " + String.join(" ",
                            Objects.requireNonNull(new File(String.valueOf(path)).list())));
                    break;
                case "cd":
                    if (command.length > 1) {
                        goToDirectory(command[1], ctx);
                    } else {
                        path = Path.of("root");
                    }
                    break;
                case "mkdir":
                    if (command.length < 2) {
                        ctx.channel().writeAndFlush("Message: Bad command");
                        break;
                    }
                    createDirectory(command[1], ctx);
                    break;
                case "rm":
                    if (command.length < 2) {
                        ctx.channel().writeAndFlush("Message: Bad command");
                        break;
                    }
                    removeFile(command[1], ctx);
                    break;
                case "nickname":
                    ctx.channel().writeAndFlush("Message: Your nickname is " + nick);
                    break;
                case "exit":
                    ctx.channel().writeAndFlush("Message: Client logged out IP: " +
                            ctx.channel().localAddress().toString());
                    connection.close();
                    ctx.channel().close();
                default:
                    ctx.channel().writeAndFlush("Message: No such command");
            }
        } else {
            ctx.channel().writeAndFlush("Message: please type auth [login] [password] for login");
        }
        ctx.fireChannelRead(builder.toString());
    }

    private void auth(ChannelHandlerContext ctx, String[] data) {
        if (data.length > 2) {
            ctx.channel().writeAndFlush("Message: Bad command");
        } else {
            try {
                connection = Connect.getConnection();
                Statement statement = connection.createStatement();
                ResultSet set = statement.executeQuery("select * from cloud.users where login='" + data[0] +
                        "' and password='" + data[1] + "'");
                if (set.next()) {
                    nick = data[0];
                    isAuth = true;
                    ctx.channel().writeAndFlush("Message: Auth complete. Your nickname is " + nick);
                }
            } catch (SQLException | ClassNotFoundException e) {
                ctx.channel().writeAndFlush("Message: " + e.getMessage());
            }
        }
    }

    private void uploading(ChannelHandlerContext ctx, String srcPath) {
        try {
            FileChannel file = new RandomAccessFile(new File(srcPath), "r").getChannel();

        } catch (FileNotFoundException e) {
            ctx.channel().writeAndFlush("Message: File not found");
        }
        /*try {
            FileChannel srcFile = new RandomAccessFile(new File(path), "r").getChannel();
            File dstFile = new File(path + filename);
            if (!dstFile.exists()) {
                dstFile.createNewFile();
            }
            ByteBuf fileLengthBuf = Unpooled.copyLong(srcFile.size());
            ByteBuf offset = Unpooled.copyLong(0);
            ByteBuf pathSize = Unpooled.copyInt(path.getBytes().length);
            ByteBuf pathByte = Unpooled.copiedBuffer(path.getBytes());
            ctx.write(fileLengthBuf);
            ctx.write(offset);
            ctx.write(pathSize);
            ctx.write(pathByte);
            ctx.flush();
            ctx.write(new ChunkedNioFile(srcFile, 0, srcFile.size(), 1024 * 1024));
            ctx.flush();
            ctx.channel().writeAndFlush("Success");
        } catch (Exception e) {
            ctx.channel().writeAndFlush("Error");
        }*/
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected " + ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    private void removeFile(String filename, ChannelHandlerContext ctx) {
        File file = new File(String.valueOf(path), filename);
        if (Files.exists(file.toPath())) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                ctx.channel().writeAndFlush(e.getMessage());
            }
            ctx.channel().writeAndFlush("Message: Success");
            return;
        }
        ctx.channel().writeAndFlush("Message: File doesn't exists");
    }

    private void goToDirectory(String dirname, ChannelHandlerContext ctx) {
        if ("..".equals(dirname)) {
            if (path.equals(Path.of("root"))) {
                return;
            }
            path = path.getParent();
            return;
        }
        Path tmp = Path.of(String.valueOf(path), dirname);
        if (!Files.isDirectory(tmp)) {
            ctx.channel().writeAndFlush("Message: Directory doesn't exists");
            return;
        }
        path = tmp;
    }

    private void createDirectory(String dirName, ChannelHandlerContext ctx) {
        if (!(dirName.trim().length() > 0 && dirName.matches("[a-zA-Z]*\\d*"))) {
            ctx.channel().writeAndFlush("Message: Bad directory name");
            return;
        }
        if (Files.isDirectory(Path.of(String.valueOf(path), dirName))) {
            ctx.channel().writeAndFlush("Directory exists");
            return;
        }
        try {
            Files.createDirectory(Path.of(String.valueOf(path), dirName));
        } catch (IOException e) {
            ctx.channel().writeAndFlush(e.getMessage());
        }
        ctx.channel().writeAndFlush("Message: Success");
    }
}
