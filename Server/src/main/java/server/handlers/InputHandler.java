package server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import server.Connect;
import server.FileInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InputHandler extends ChannelInboundHandlerAdapter {
    public static final String LS_COMMAND = "\tls view all files and directories\n";
    public static final String MKDIR_COMMAND = "\tmkdir [dirname] create directory\n";
    public static final String CD_COMMAND = "\tcd go to directory\n";
    public static final String RM_COMMAND = "\trm [filename] delete file\n";
    public static final String NICKNAME_COMMAND = "\tnickname show your nickname\n";
    public static final String UPLOAD = "\tupload [path] [filename] upload your file in current directory on server\n";
    private static String nick = "admin";
    private static Path path = Path.of("root/" + nick);
    private Connection connection = null;
    private ByteBuf buf;
    private boolean isAuth = true;
    private List<FileInfo> fileInfoList = new ArrayList<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected " + ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws SQLException {
        buf = (ByteBuf) msg;
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
                    buf.clear().writeBytes(("Message: \n" + LS_COMMAND + MKDIR_COMMAND + CD_COMMAND + RM_COMMAND +
                            UPLOAD + NICKNAME_COMMAND).getBytes(StandardCharsets.UTF_8));
                    ctx.channel().writeAndFlush(buf);
                    break;
                case "upload":
                    if (command.length < 2) {
                        ctx.channel().writeAndFlush(buf.clear()
                                .writeBytes("Message: Bad command".getBytes(StandardCharsets.UTF_8)));
                    } else {
                        uploading(ctx, command[1]);
                    }
                    break;
                case "download":
                    if (command.length < 2) {
                        ctx.channel().writeAndFlush(buf.clear()
                                .writeBytes("Message: Bad command".getBytes(StandardCharsets.UTF_8)));
                    } else {
                        downloading(ctx, command[1]);
                    }
                case "ls":
                    showFiles(ctx);
                    break;
                case "cd":
                    if (command.length > 1) {
                        goToDirectory(command[1], ctx);
                    } else {
                        path = Path.of("root/" + nick);
                    }
                    break;
                case "mkdir":
                    if (command.length < 2) {
                        ctx.channel().writeAndFlush(buf.clear()
                                .writeBytes("Message: Bad command".getBytes(StandardCharsets.UTF_8)));
                        break;
                    }
                    createDirectory(command[1], ctx);
                    break;
                case "rm":
                    if (command.length < 2) {
                        ctx.channel().writeAndFlush(buf.clear()
                                .writeBytes("Message: Bad command".getBytes(StandardCharsets.UTF_8)));
                        break;
                    }
                    removeFile(command[1], ctx);
                    break;
                case "nickname":
                    ctx.channel().writeAndFlush(buf.clear()
                            .writeBytes(("Message: Your nickname is " + nick).getBytes(StandardCharsets.UTF_8)));
                    break;
                case "exit":
                    ctx.channel().close();
                    if (connection != null) {
                        connection.close();
                    }
                    break;
            }
        } else {
            ctx.channel().writeAndFlush(buf.clear()
                    .writeBytes("Message: please type auth [login] [password] for login".getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void showFiles(ChannelHandlerContext ctx){
        try {
            fileInfoList = Files.list(path).map(FileInfo::new).collect(Collectors.toList());
            if (fileInfoList.size() > 0){
                StringBuilder fileInfo = new StringBuilder();
                fileInfo.append("List:");
                for (FileInfo info : fileInfoList) {
                    fileInfo.append(info.getFilename() + " " +
                            info.getType() + " " +
                            info.getLastModified() + "%");
                }
                ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(fileInfo.toString().getBytes(StandardCharsets.UTF_8)));
            } else {
                ctx.channel().writeAndFlush(buf.clear().writeBytes("List:!!".getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloading(ChannelHandlerContext ctx, String s) {
        Path path = Path.of(s);
        try {
            ctx.channel().writeAndFlush(buf.clear().writeBytes(Files.readAllBytes(path)));
        } catch (IOException e) {
            ctx.channel().writeAndFlush(buf.clear().writeBytes(e.getMessage().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void auth(ChannelHandlerContext ctx, String[] data) {
        if (data.length > 2) {
            ctx.channel().writeAndFlush(buf.clear()
                    .writeBytes("Message: Bad command".getBytes(StandardCharsets.UTF_8)));
        } else {
            try {
                connection = Connect.getConnection();
                Statement statement = connection.createStatement();
                ResultSet set = statement.executeQuery("select * from cloud.users where login='" + data[0] +
                        "' and password='" + data[1] + "'");
                if (set.next()) {
                    nick = data[0];
                    isAuth = true;
                    ctx.channel().writeAndFlush(buf.clear()
                            .writeBytes(("Message: Auth complete. Your nickname is " + nick)
                                    .getBytes(StandardCharsets.UTF_8)));
                    ctx.channel().writeAndFlush(buf.clear()
                            .writeBytes(("Nick:" + nick).getBytes(StandardCharsets.UTF_8)));
                }
            } catch (SQLException | ClassNotFoundException e) {
                ctx.channel().writeAndFlush(buf.clear()
                        .writeBytes(("Message: " + e.getMessage()).getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private void uploading(ChannelHandlerContext ctx, String srcPath) {
        try {
            String[] fileBytes = srcPath.split("%");
            Path path = Path.of(InputHandler.path + "/" + fileBytes[0]);
            long offset = 0;
            File f = new File(String.valueOf(path));
            if (!Files.exists(path)) {
                Files.createFile(path);
            } else {
                offset = f.length();
            }
            RandomAccessFile file = new RandomAccessFile(f, "rw");
            file.seek(offset);
            file.writeBytes(fileBytes[1]);
            ctx.writeAndFlush(file);
        } catch (
                IOException e) {
            e.printStackTrace();
        }
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
                ctx.channel().writeAndFlush(buf.clear()
                        .writeBytes(("Message: " + e.getMessage()).getBytes(StandardCharsets.UTF_8)));
            }
            ctx.channel().writeAndFlush(buf.clear()
                    .writeBytes("Message: Success".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        ctx.channel().writeAndFlush(buf.clear()
                .writeBytes("Message: File doesn't exists".getBytes(StandardCharsets.UTF_8)));
    }

    private void goToDirectory(String dirname, ChannelHandlerContext ctx) {
        if ("..".equals(dirname)) {
            if (path.equals(Path.of("root/" + nick))) {
                return;
            }
            path = path.getParent();
            return;
        }
        Path tmp = Path.of(String.valueOf(path), dirname);
        if (!Files.isDirectory(tmp)) {
            ctx.channel().writeAndFlush(buf.clear()
                    .writeBytes("Message: Directory doesn't exists".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        path = tmp;
    }

    private void createDirectory(String dirName, ChannelHandlerContext ctx) {
        if (!(dirName.trim().length() > 0 && dirName.matches("[a-zA-Z]*\\d*"))) {
            ctx.channel().writeAndFlush(buf.clear()
                    .writeBytes("Message: Bad directory name".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        if (Files.isDirectory(Path.of(String.valueOf(path), dirName))) {
            ctx.channel().writeAndFlush(buf.clear()
                    .writeBytes("Message: Directory exists".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        try {
            Files.createDirectory(Path.of(String.valueOf(path), dirName));
        } catch (IOException e) {
            ctx.channel().writeAndFlush(buf.clear()
                    .writeBytes(("Message: " + e.getMessage()).getBytes(StandardCharsets.UTF_8)));
        }
        ctx.channel().writeAndFlush(buf.clear()
                .writeBytes("Message: Success".getBytes(StandardCharsets.UTF_8)));
    }
}
