package client.explorer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileInfo {
    private final String filename;
    private final FileType type;
    private final LocalDateTime lastModified;

    /**
     * Сбор данных о файле
     * @param path Path to file
     */
    public FileInfo(Path path) {
        try {
            this.filename = path.getFileName().toString();
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(),
                    ZoneOffset.ofHours(0));
        } catch (IOException e) {
            throw new RuntimeException("Bad file");
        }
    }

    public String getFilename() {
        return filename;
    }

    public FileType getType() {
        return type;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    /**
     * Используется для инициализации типа файла (файл или директория)
     */
    public enum FileType {
        FILE("File"), DIRECTORY("Directory");

        private final String name;

        FileType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
