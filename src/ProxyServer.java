import java.net.*;
import java.io.*;

public class ProxyServer {

    protected static int port = 8888;
    protected static ServerSocket serverSocket = null;

    public static void main(String[] args) throws IOException {

        boolean isListening = true;

        BlackList.init();

        if (BlackList.isOpen)
            System.out.println("Read Black list successfully.");
        else
            System.out.println("Cannot read Black list. Proxy Server will run without Black list.");

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Proxy Server started on port: " + port);
        } catch (IOException e) {
            System.err.println("Error: Proxy Server couldn't started on port: " + port);
            System.exit(-1);
        }

        while (isListening) {
            new ProxyThread(serverSocket.accept()).start();
        }

        serverSocket.close();

    }
}
