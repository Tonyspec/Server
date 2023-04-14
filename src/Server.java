import java.net.*;
import java.io.*;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(3999);
        System.out.println("Server initialised.");

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());
                Thread thread = new Thread(new ClientHandler(socket));
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

