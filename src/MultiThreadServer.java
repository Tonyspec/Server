import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(3999);
        System.out.println("Server is listening on port 3999");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            Thread clientThread = new Thread(new ClientHandler(clientSocket));
            clientThread.start();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private static final String SERVER_MOVE = "102 MOVE\u0007\u0008";
    private static final String SERVER_TURN_LEFT = "103 TURN LEFT\u0007\u0008";
    private static final String SERVER_TURN_RIGHT = "104 TURN RIGHT\u0007\u0008";
    private static final String SERVER_PICK_UP = "105 GET MESSAGE\u0007\u0008";
    private static final String SERVER_LOGOUT = "106 LOGOUT\u0007\u0008";
    private static final String SERVER_KEY_REQUEST = "107 KEY REQUEST\u0007\u0008";
    private static final String SERVER_OK = "200 OK\u0007\u0008";
    private static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED\u0007\u0008";
    private static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR\u0007\u0008";
    private static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR\u0007\u0008";
    private static final String SERVER_KEY_OUT_OF_RANGE_ERROR = "303 KEY OUT OF RANGE\u0007\u0008";

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            Integer iteration = 0, code = 0, key = 0, ascii_value = 0, clientCode = 0;
            Integer oldX = 0;
            Integer oldY = 0;
            String name = null;
            while (true) {
                byte[] buffer = new byte[1024];
                int totalBytesRead = 0;
                String message = "";
                while (true) {
                    clientSocket.setSoTimeout(1000);
                    try{
                        int bytesRead = inputStream.read(buffer);
                        if (bytesRead == -1) {
                            System.out.println("No data received from client");
                            break;
                        }
                        String partialMessage = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                        totalBytesRead += bytesRead;
                        message += partialMessage;
                        if (message.endsWith("\u0007\u0008") || totalBytesRead >= 20) {
                            break;
                        }
                    }
                    catch(SocketTimeoutException e){
                        clientSocket.close();
                        System.out.println("Timeout reached!");
                    }

                }
                if (!message.endsWith("\u0007\u0008")) {
                    sendErrorMessage(clientSocket);
                    System.out.println("Invalid message format received from client: " + message);
                    clientSocket.close();
                    break;
                }
                if(totalBytesRead > 20){
                    outputStream.write(SERVER_SYNTAX_ERROR.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    clientSocket.close();
                    System.out.println("Long name!");
                }
                System.out.println("Client: "+message+ " iteration is " + iteration);
                // handle client's request


                if(iteration == 0){
                    // send key request
                    name = message;
                    outputStream.write(SERVER_KEY_REQUEST.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    System.out.println("SERVER_KEY_REQUEST sent");
                }
                else if (iteration == 1) {
                    System.out.println("2" +message);
                    for (int i = 0; i < name.length() - 2; i++) {
                        //System.out.println("Adding " + name.charAt(i));
                        ascii_value += name.charAt(i);
                    }
//                    System.out.println("Ascii is " + code);
                    System.out.println("Ascii is " + ascii_value);
                    code = (ascii_value * 1000) % 65536;
                    System.out.println("Ascii is " + code);
                    try{
                        key = Integer.parseInt(String.valueOf(message.charAt(0)));
                    }
                    catch (NumberFormatException e){
                        sendErrorMessage(clientSocket);
                        clientSocket.close();
                        System.out.println("Not a number!");
                    }
//                    System.out.println("Key is " + key);
                    switch (key) {
                        case 0 -> {
                            code = (code + 23019) % 65536;
                            System.out.println("Case one");
                        }
                        case 1 -> code = (code + 32037) % 65536;
                        case 2 -> code = (code + 18789) % 65536;
                        case 3 -> code = (code + 16443) % 65536;
                        case 4 -> code = (code + 18189) % 65536;
                        default -> {
                            outputStream.write(SERVER_KEY_OUT_OF_RANGE_ERROR.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                            clientSocket.close();
                            System.out.println("SERVER_KEY_OUT_OF_RANGE_ERROR sent");
                        }
                    }
                    System.out.println("Hash is " + code);
                    String hashOutput = code + "\u0007\u0008";
                    outputStream.write(hashOutput.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    System.out.println("KEY_CONFIRMATION sent");
                } else if (iteration == 2) {
                    String withoutSpaces = message.replaceAll("\\s+", "");
                    if(withoutSpaces.length() > 7){
                        sendErrorMessage(clientSocket);
                        clientSocket.close();
                        System.out.println("Long confirmation code!");
                    }
                    try{
                        clientCode = Integer.parseInt(message.substring(0,message.length()-2));
                    }
                    catch (NumberFormatException e){
                        sendErrorMessage(clientSocket);
                        clientSocket.close();
                        System.out.println("Not a number!");
                    }

                    //System.out.println("Client code is " + clientCode);
                    Integer codeForAuth = (ascii_value * 1000) % 65536;
                    //System.out.println("Ascii is " + codeForAuth);
                    switch (key) {
                        case 0 -> codeForAuth = (codeForAuth + 32037) % 65536;
                        case 1 -> codeForAuth = (codeForAuth + 29295) % 65536;
                        case 2 -> codeForAuth = (codeForAuth + 13603) % 65536;
                        case 3 -> codeForAuth = (codeForAuth + 29533) % 65536;
                        case 4 -> codeForAuth = (codeForAuth + 21952) % 65536;
                        default -> {
                            outputStream.write(SERVER_KEY_OUT_OF_RANGE_ERROR.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                            clientSocket.close();
                            System.out.println("SERVER_KEY_OUT_OF_RANGE_ERROR sent");
                            System.out.println("Wrong key!");
                        }
                    }
                    //System.out.println("Client code result is " + codeForAuth);
                    //System.out.println("Need " + clientCode);
                    if(codeForAuth.equals(clientCode)){
                        outputStream.write(SERVER_OK.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                    else{
                        outputStream.write(SERVER_LOGIN_FAILED.getBytes(StandardCharsets.UTF_8));
                        clientSocket.close();
                        outputStream.flush();
                        break;
                    }
                    outputStream.write(SERVER_MOVE.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
//                else {
//
////                    String[] parts = message.split("\\s+|\u0007|\u0008"); // split by whitespace
//                    oldX = getX(message);
//                    oldY = getY(message);
//                    System.out.println("Coordinates are: " + oldX + " and " + oldY);
//                    outputStream.write(SERVER_TURN_LEFT.getBytes(StandardCharsets.UTF_8));
//                    outputStream.flush();
//                    while (true) {
//                        // Send MOVE command to move forward
//                        // and get the response with new coordinates
//                        outputStream.write(SERVER_MOVE.getBytes(StandardCharsets.UTF_8));
//                        outputStream.flush();
//                        int newX = getX(message);
//                        int newY = getY(message);
//
//                        // Check if the new coordinates are the same as the previous ones
//                        if (newX == oldX && newY == oldY) {
//                            // If they are the same, the coordinate is blocked
//                            // so we turn right and try to move forward again
//                            sendCommand("TURN_RIGHT");
//                            response = sendCommand("MOVE");
//                            parts = response.split(" ");
//                            newX = Integer.parseInt(parts[0]);
//                            newY = Integer.parseInt(parts[1]);
//                        }
//
//                        // Update the current and previous coordinates
//                        prevX = currentX;
//                        prevY = currentY;
//                        currentX = newX;
//                        currentY = newY;
//
//                        // Check if we have reached the target coordinates
//                        if (currentX == 0 && currentY == 0) {
//                            break;
//                        }
//
//                        // Calculate the direction to the target coordinates
//                        int dx = 0;
//                        int dy = 0;
//                        if (currentX > 0) {
//                            dx = 1;
//                        } else if (currentX < 0) {
//                            dx = -1;
//                        }
//                        if (currentY > 0) {
//                            dy = 1;
//                        } else if (currentY < 0) {
//                            dy = -1;
//                        }
//
//                        // Turn to face the target coordinates
//                        if (dx > 0) {
//                            sendCommand("TURN_RIGHT");
//                        } else if (dx < 0) {
//                            sendCommand("TURN_LEFT");
//                        }
//                        if (dy > 0) {
//                            sendCommand("TURN_RIGHT");
//                        } else if (dy < 0) {
//                            sendCommand("TURN_LEFT");
//                        }
//                    }
//                }
                iteration++;
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
        output.write(SERVER_SYNTAX_ERROR.getBytes());
        clientSocket.close();
        System.out.println("Server sent error message to client: " + SERVER_SYNTAX_ERROR);
    }
    private Integer getX(String message){
        String[] parts = message.split("\\s+|\u0007|\u0008");
        return Integer.parseInt(parts[1]);
    }
    private Integer getY(String message){
        String[] parts = message.split("\\s+|\u0007|\u0008");
        return Integer.parseInt(parts[2]);
    }
//    private static void sendCommand(String direction){
//        if(direction.equals("MOVE")){
//            outputStream.write(SERVER_TURN_LEFT.getBytes(StandardCharsets.UTF_8));
//            outputStream.flush();
//        }
//    }
}
