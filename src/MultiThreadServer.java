import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private String buffer = "";
    Pattern pattern = Pattern.compile("^OK (-?\\d+) (-?\\d+)$");
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
            String firstMessage = null;
            while (true) {
                System.out.println("Big cycle");
                String message = loadInput(inputStream, outputStream, 20);
                if (iteration == 0) {
                    // send key request
                    name = message;
                    System.out.println("Name " + name);
                    outputStream.write(SERVER_KEY_REQUEST.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    System.out.println("SERVER_KEY_REQUEST sent");
                } else if (iteration == 1) {
                    System.out.println("2" + message);
                    for (int i = 0; i < name.length() - 2; i++) {
                        //System.out.println("Adding " + name.charAt(i));
                        ascii_value += name.charAt(i);
                    }
//                    System.out.println("Ascii is " + code);
                    System.out.println("Ascii is " + ascii_value);
                    code = (ascii_value * 1000) % 65536;
                    System.out.println("Ascii is " + code);
                    try {
                        key = Integer.parseInt(String.valueOf(message.charAt(0)));
                    } catch (NumberFormatException e) {
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
                    if (withoutSpaces.length() > 7) {
                        sendErrorMessage(clientSocket);
                        clientSocket.close();
                        System.out.println("Long confirmation code!");
                    }
                    try {
                        clientCode = Integer.parseInt(message.substring(0, message.length() - 2));
                    } catch (NumberFormatException e) {
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
                    if (codeForAuth.equals(clientCode)) {
                        outputStream.write(SERVER_OK.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    } else {
                        outputStream.write(SERVER_LOGIN_FAILED.getBytes(StandardCharsets.UTF_8));
                        clientSocket.close();
                        outputStream.flush();
                        break;
                    }
//                    outputStream.write(SERVER_MOVE.getBytes(StandardCharsets.UTF_8));
//                    outputStream.flush();
//                    System.out.println("last message");
//                    message = loadInput(inputStream, outputStream);
//                    message = message.substring(0, message.length() - 2);
//                    String[] parts = message.split(" ");
//                    oldX = Integer.parseInt(parts[1]);
//                    oldY = Integer.parseInt(parts[2]);
//                    System.out.println("last message");
                        break;
                }
                iteration++;
            }
            int x = 0; // souřadnice x
            int y = 0; // souřadnice y
            int prevX = 0; // předchozí souřadnice x
            int prevY = 0; // předchozí souřadnice y
            boolean firstMove = true; // zda se jedná o první pohyb
            boolean obstacleHit = false; // zda robot narazil na překážku
            int[] coords = new int[2];
            String message = null;
            while (true) {
                if(firstMove){
                    outputStream.write(SERVER_TURN_LEFT.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    message = loadInput(inputStream, outputStream, 12);
                    getCoordinates(message, coords);
                    prevX = coords[0];
                    prevY = coords[1];
                    outputStream.write(SERVER_MOVE.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    message = loadInput(inputStream, outputStream, 12);
                    getCoordinates(message, coords);
                    x = coords[0];
                    y = coords[1];
                    if(x == prevX && y == prevY){
                        outputStream.write(SERVER_TURN_RIGHT.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                        outputStream.write(SERVER_MOVE.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                        message = loadInput(inputStream, outputStream, 12);
                        getCoordinates(message, coords);
                        x = coords[0];
                        y = coords[1];
                    }
                    firstMove = false;
                }
                else{
                    message = loadInput(inputStream, outputStream, 12);
                    System.out.println("Message is " + message);
                    getCoordinates(message, coords);
                    x = coords[0];
                    y = coords[1];
                }
                // Kontrola, zda robot dorazil na cíl
                if (x == 0 && y == 0) {
                    outputStream.write(SERVER_PICK_UP.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    String secret = loadInput(inputStream, outputStream, 100);
                    System.out.println("Secret is " + secret);
                    outputStream.write(SERVER_LOGOUT.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    break;
                }
                // Zjištění směru pohybu robota a provedení pohybu daným směrem
                System.out.println("Coordinates " + " x " + x + " y " + y + " prevX " + prevX + " prevY " + prevY);
                String directionNeed = getDirection(obstacleHit, x, y, prevX, prevY);
                System.out.println("Direction need is " + directionNeed);
                String directionFacing = getFacingSide(x, y, prevX, prevY);
                System.out.println("Direction facing is " + directionFacing);
                move(directionNeed, directionFacing, inputStream, outputStream);
                // Kontrola, zda robot narazil na překážku
                if (x == prevX && y == prevY) {
                    obstacleHit = true;
                    System.out.println("Bang");

                } else {
                    obstacleHit = false;
                    prevX = x;
                    prevY = y;
                }
            }

            // close the connection
            clientSocket.close();
            System.out.println("Connection with client closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Metoda pro zjisteni jakym smerem se ma robot pohybovat
    private String getFacingSide(int x, int y, int prevX, int prevY) {
        if (x > prevX) {
            return "Right";
        } else if (x < prevX) {
            return "Left";
        } else if (y < prevY) {
            return "Down";
        } else {
            return "Up";
        }
    }
    private String getDirection(boolean obstacleHit, int x, int y, int prevX, int prevY) {
        //Robot nenarazil
        if(!obstacleHit){
            if (Math.abs(x) > Math.abs(y)) {
                return (x > 0 ? "Left" : "Right");
            } else {
                return (y > 0 ? "Down" : "Up");
            }
        }
        else{
            //Robot narazil
            if (Math.abs(x) <= Math.abs(y)) {
                return (x > 0 ? "Left" : "Right");
            } else {
                return (y > 0 ? "Down" : "Up");
            }
        }

    }
    // Metoda pro provedení jednoho kroku robota daným směrem
    private boolean move(String directionFacing, String directionNeeded, InputStream inputStream, OutputStream outputStream) throws IOException {
        int directionOld = 0;
        int directionNew = 0;
        int rotations = 0;
        String response = null;
        if(directionNeeded.equals("Up"))
            directionNew = 1;
        else if(directionNeeded.equals("Left"))
            directionNew = 2;
        else if(directionNeeded.equals("Down"))
            directionNew = 3;
        else if(directionNeeded.equals("Right"))
            directionNew = 4;
        if(directionFacing.equals("Up"))
            directionOld = 1;
        else if(directionFacing.equals("Left"))
            directionOld = 2;
        else if(directionFacing.equals("Down"))
            directionOld = 3;
        else if(directionFacing.equals("Right"))
            directionOld = 4;
        //---------------------
        if(directionNew == directionOld)
            ;
        else if (directionNew > directionOld) {
            rotations = directionNew - directionOld;
            for(int i = 0; i < rotations; i++){
                outputStream.write(SERVER_TURN_RIGHT.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                System.out.println("Turned right");
                response = loadInput(inputStream, outputStream, 12);
                if(!response.startsWith("OK"))
                    return false;
            }
        } else if (directionNew < directionOld) {
            rotations = directionOld - directionNew;
            for(int i = 0; i < rotations; i++){
                outputStream.write(SERVER_TURN_LEFT.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                System.out.println("Turned left");
                response = loadInput(inputStream, outputStream, 12);
                if(!response.startsWith("OK"))
                    return false;
            }
        }
        outputStream.write(SERVER_MOVE.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        return true;
    }
    // Metoda pro zjištění souřadnic
    private void getCoordinates(String message, int[] coords) throws IOException {
        message = message.substring(0, message.length() - 2);
        System.out.println("Was " + message);
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find() || matcher.groupCount() != 2) {
            sendErrorMessage(clientSocket);
            return;
        }
        coords[0] = Integer.parseInt(matcher.group(1));
        coords[1] = Integer.parseInt(matcher.group(2));
    }

    // Metoda pro zjištění směru pohybu robota

    private void updateXandY(String message, int[] coords) {
        message = message.substring(0, message.length() - 2);
        String[] parts = message.split(" ");
        coords[0] = Integer.parseInt(parts[1]);
        coords[1] = Integer.parseInt(parts[2]);
    }

    private String loadInput(InputStream inputStream, OutputStream outputStream, int maxLenght) throws IOException {
        int totalBytesRead = 0;
        String message = "";

        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        while (!message.endsWith("\u0007\u0008") && message.length() < 100) {
            clientSocket.setSoTimeout(1500);
            try{
                int bytesRead = 0;
                if(buffer.length() == 0){
                    byte[] buf = new byte[1024];
                    bytesRead = inputStream.read(buf);
                    if (bytesRead == -1) {
                        System.out.println("No data received from client");
                        break;
                    }
                    buffer = new String(buf, 0, bytesRead, StandardCharsets.UTF_8);
                }

                if(buffer.startsWith("\u0007\u0008")){
                    message += "\u0007\u0008";
                    buffer = buffer.substring(2);
                    System.out.println(1);
                    continue;
                }
                if(message.endsWith("\u0007")&& buffer.startsWith("\u0008")){
                    message += "\u0008";
                    buffer = buffer.substring(1);
                    System.out.println(2);
                    continue;
                }

                message += buffer.split(new String("\u0007\u0008"),2)[0];
                if(message.endsWith("\u0007\u0008"))
                    break;
                System.out.println("Message " + message);
                buffer = buffer.substring(buffer.split("\u0007\u0008",2)[0].length());
//                String partialMessage = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                totalBytesRead += bytesRead;
            }
            catch(IOException e){
                clientSocket.close();
                System.out.println("Timeout reached!");
            }

        }
        if (!message.endsWith("\u0007\u0008")) {
            sendErrorMessage(clientSocket);
            System.out.println("Invalid message format received from client: " + message);
            clientSocket.close();
        }
//        if(totalBytesRead > maxLenght){
//            outputStream.write(SERVER_SYNTAX_ERROR.getBytes(StandardCharsets.UTF_8));
//            outputStream.flush();
//            clientSocket.close();
//            System.out.println("Long message!");
//        }
        //System.out.println("Client: "+message+ " iteration is " + iteration);
        // handle client's request
        return message;
    }
    private static void sendErrorMessage(Socket clientSocket) throws IOException {
        OutputStream output = clientSocket.getOutputStream();
        output.write(SERVER_SYNTAX_ERROR.getBytes(StandardCharsets.UTF_8));
        output.flush();
        clientSocket.close();
        System.out.println("Server sent error message to client: " + SERVER_SYNTAX_ERROR);
    }
}
