package client;

public class FileOnServer {
    private final String filename;
    private final String type;
    private final String lastModified;

    FileOnServer(String fileName, String fileType, String lastModified) {
        this.filename = fileName;
        this.type = fileType;
        this.lastModified = lastModified;
    }

    public String getFilename() {
        return filename;
    }

    public String getType() {
        return type;
    }

    public String getLastModified() {
        return lastModified;
    }

}
