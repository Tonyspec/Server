import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(3999);
        System.out.println("Server is listening on port 1234");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            Thread clientThread = new Thread(new clientHandler(clientSocket));
            clientThread.start();
        }
    }
}

class clientHandler implements Runnable {
    private Socket clientSocket;

    public clientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            // send initial message to client
            String initialMessage = "107 KEY REQUEST\u0007\u0008";
            outputStream.write(initialMessage.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            System.out.println("Sent initial message to client: " + initialMessage);


            while (true) {
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    System.out.println("No data received from client");
                    break;
                }
                String message = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                if (!message.endsWith("\u0007\u0008")) {
                    sendErrorMessage(clientSocket);
                    System.out.println("Invalid message format received from client: " + message);
                    clientSocket.close();
                    break;
                }

                // handle client's requests
                // ...
            }
            // close the connection
            clientSocket.close();
            System.out.println("Connection with client closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendErrorMessage(Socket clientSocket) throws IOException {
        OutputStream output = clientSocket.getOutputStream();
        String errorMessage = "SERVER_SYNTAX_ERROR\u0007\u0008";
        output.write(errorMessage.getBytes());
        System.out.println("Server sent error message to client: " + errorMessage);
    }
}
