package server;

public class RunServer {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 4000;
        new Server(port);
    }
}
