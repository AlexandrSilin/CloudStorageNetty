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
import java.util.List;
import java.util.stream.Collectors;

public class InputHandler extends ChannelInboundHandlerAdapter {
    public static final String LS_COMMAND = "\tls view all files and directories\n";
    public static final String MKDIR_COMMAND = "\tmkdir [dirname] create directory\n";
    public static final String CD_COMMAND = "\tcd go to directory\n";
    public static final String RM_COMMAND = "\trm [filename] delete file\n";
    public static final String NICKNAME_COMMAND = "\tnickname show your nickname\n";
    public static final String UPLOAD = "\tupload [path] [filename] upload your file in current directory on server\n";
    private static String nick = "";
    private static Path path = Path.of("root/" + nick);
    private Connection connection = null;
    private ByteBuf buf;
    private boolean isAuth = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected " + ctx.channel());
    }

    /**
     * Обработчик входящих сообщений
     * @param ctx ChannelHandlerContext
     * @param msg Object received message
     * @throws SQLException
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws SQLException {
        buf = (ByteBuf) msg;
        StringBuilder builder = new StringBuilder();
        char c;
        while (buf.isReadable()) {
            c = (char) buf.readByte();
            if (c == '%') {
                break;
            }
            builder.append(c);
        }
        String[] command = builder.toString().trim().split(" ", 2);
        if (command[0].equals("auth")) {
            auth(ctx, command[1].split(" "));
        }
        if (isAuth) {
            switch (command[0]) {
                case "--help":
                    buf.clear().writeBytes(("Message: \n" + LS_COMMAND + MKDIR_COMMAND + CD_COMMAND + RM_COMMAND +
                            UPLOAD + NICKNAME_COMMAND + "%info").getBytes(StandardCharsets.UTF_8));
                    ctx.writeAndFlush(buf);
                    break;
                case "upload":
                    if (command.length < 2) {
                        ctx.writeAndFlush(buf.clear()
                                .writeBytes("Message: Bad command%error".getBytes(StandardCharsets.UTF_8)));
                    } else {
                        uploading(ctx, command[1], buf);
                    }
                    break;
                case "download":
                    if (command.length < 2) {
                        ctx.writeAndFlush(buf.clear()
                                .writeBytes("Message: Bad command%error".getBytes(StandardCharsets.UTF_8)));
                    } else {
                        downloading(ctx, command[1]);
                    }
                    break;
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
                        ctx.writeAndFlush(buf.clear()
                                .writeBytes("Message: Bad command%error".getBytes(StandardCharsets.UTF_8)));
                        break;
                    }
                    createDirectory(command[1], ctx);
                    break;
                case "rm":
                    if (command.length < 2) {
                        ctx.writeAndFlush(buf.clear()
                                .writeBytes("Message: Bad command%error".getBytes(StandardCharsets.UTF_8)));
                        break;
                    }
                    removeFile(command[1], ctx);
                    break;
                case "exit":
                    ctx.channel().close();
                    if (connection != null) {
                        connection.close();
                    }
                    break;
            }
        } else {
            ctx.writeAndFlush(buf.clear()
                    .writeBytes("Message: please fulfill form auth [login] [password] for login%warning".getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * Отправка списка файлов хранящихся в каталоге
     * @param ctx ChannelHandlerContext
     */
    private void showFiles(ChannelHandlerContext ctx) {
        try {
            List<FileInfo> fileInfoList = Files.list(path).map(FileInfo::new).collect(Collectors.toList());
            if (fileInfoList.size() > 0) {
                StringBuilder fileInfo = new StringBuilder();
                fileInfo.append("List:");
                for (FileInfo info : fileInfoList) {
                    fileInfo.append(info.getFilename()).append(" ")
                            .append(info.getType()).append(" ")
                            .append(info.getLastModified()).append("%");
                }
                ctx.writeAndFlush(Unpooled.wrappedBuffer(fileInfo.toString().getBytes(StandardCharsets.UTF_8)));
            } else {
                ctx.writeAndFlush(buf.clear().writeBytes("List:!!".getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправка файла с сервера
     * @param ctx ChannelHandlerContext
     * @param s String path to file
     */
    private void downloading(ChannelHandlerContext ctx, String s) {
        try {
            Path path = Path.of(s);
            File file = new File(String.valueOf(path));
            RandomAccessFile src = new RandomAccessFile(file, "r");
            String command = "File:" + file.getName() + "%";
            if (file.length() < Integer.MAX_VALUE) {
                byte[] fileBytes = new byte[(int) file.length()];
                src.readFully(fileBytes);
                byte[] bufCommand = new byte[command.length()];
                for (int j = 0; j < command.length(); j++) {
                    bufCommand[j] = (byte) command.charAt(j);
                }
                ByteBuf buf = Unpooled.copiedBuffer(bufCommand, fileBytes);
                ctx.writeAndFlush(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Авторизация на сервере
     * @param ctx ChannelHandlerContext
     * @param data String[] user's data
     */
    private void auth(ChannelHandlerContext ctx, String[] data) {
        if (data.length > 2) {
            ctx.writeAndFlush(buf.clear()
                    .writeBytes("Message: Bad command%error".getBytes(StandardCharsets.UTF_8)));
        } else {
            try {
                connection = Connect.getConnection();
                Statement statement = connection.createStatement();
                ResultSet set = statement.executeQuery("select * from cloud.users where login='" + data[0] +
                        "' and password='" + data[1] + "'");
                if (set.next()) {
                    nick = data[0];
                    isAuth = true;
                    ctx.writeAndFlush(buf.clear()
                            .writeBytes(("nick:" + nick).getBytes(StandardCharsets.UTF_8)));
                    path = Path.of("root/" + nick);
                }
            } catch (SQLException | ClassNotFoundException e) {
                ctx.writeAndFlush(buf.clear()
                        .writeBytes(("Message: " + e.getMessage() + "%error").getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    /**
     * Загрузка файлов на сервер
     * @param ctx ChannelHandlerContext
     * @param srcPath String filename
     * @param buf ByteBuf file data
     */
    private void uploading(ChannelHandlerContext ctx, String srcPath, ByteBuf buf) {
        try {
            String[] fileBytes = srcPath.split("%");
            Path path = Path.of(InputHandler.path + "/" + fileBytes[0]);
            File f = new File(String.valueOf(path));
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            RandomAccessFile file = new RandomAccessFile(f, "rw");
            while (buf.isReadable()) {
                file.write(buf.readByte());
            }
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

    /**
     * Удаление файлов на сервере
     * @param filename String filename
     * @param ctx ChannelHandlerContext
     */
    private void removeFile(String filename, ChannelHandlerContext ctx) {
        File file = new File(String.valueOf(path), filename);
        if (Files.exists(file.toPath())) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                ctx.writeAndFlush(buf.clear()
                        .writeBytes(("Message: " + e.getMessage() + "%error").getBytes(StandardCharsets.UTF_8)));
            }
            ctx.writeAndFlush(buf.clear()
                    .writeBytes("Message: Success%info".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        ctx.writeAndFlush(buf.clear()
                .writeBytes("Message: File doesn't exists%error".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Переход в каталог
     * @param dirname String catalog name
     * @param ctx ChannelHandlerContext
     */
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
            ctx.writeAndFlush(buf.clear()
                    .writeBytes("Message: Directory doesn't exists%error".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        path = tmp;
    }

    /**
     * Создание каталога на сервере
     * @param dirName String catalog name
     * @param ctx ChannelHandlerContext
     */
    private void createDirectory(String dirName, ChannelHandlerContext ctx) {
        if (!(dirName.trim().length() > 0 && dirName.matches("[a-zA-Z]*\\d*"))) {
            ctx.writeAndFlush(buf.clear()
                    .writeBytes("Message: Bad directory name%info".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        if (Files.isDirectory(Path.of(String.valueOf(path), dirName))) {
            ctx.writeAndFlush(buf.clear()
                    .writeBytes("Message: Directory exists%error".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        try {
            Files.createDirectory(Path.of(String.valueOf(path), dirName));
        } catch (IOException e) {
            ctx.writeAndFlush(buf.clear()
                    .writeBytes(("Message: " + e.getMessage() + "%error").getBytes(StandardCharsets.UTF_8)));
        }
        ctx.writeAndFlush(buf.clear()
                .writeBytes("Message: Success%info".getBytes(StandardCharsets.UTF_8)));
    }
}
