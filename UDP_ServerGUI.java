import java.io.*;
import java.net.*;

public class UDP_ServerGUI {
    public static void main(String[] args) {
        // Server details
        final int PORT = 5000;
        final int MAXLINE = 1000;

        DatagramSocket socket = null;

        try {
            // Create a UDP socket and bind to the specified port
            socket = new DatagramSocket(PORT);
            System.out.println("Server is running and waiting for messages...");

            while (true) {
                // Buffer to receive client messages
                byte[] receiveBuffer = new byte[MAXLINE];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                // Receive message from client
                socket.receive(receivePacket);
                String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                System.out.println("Received: " + clientMessage);

                // Handle client commands
                if (clientMessage.startsWith("FILE:")) {
                    // Handle file reception
                    String fileName = clientMessage.substring(5);
                    System.out.println("Receiving file: " + fileName);

                    // Receive file data
                    FileOutputStream fos = new FileOutputStream("server_" + fileName);
                    boolean receiving = true;
                    while (receiving) {
                        socket.receive(receivePacket);
                        String receivedChunk = new String(receivePacket.getData(), 0, receivePacket.getLength());

                        if ("FILE_DONE".equals(receivedChunk)) {
                            System.out.println("File " + fileName + " received successfully.");
                            receiving = false;
                        } else {
                            fos.write(receivePacket.getData(), 0, receivePacket.getLength());
                        }
                    }
                    fos.close();

                } else if (clientMessage.startsWith("REQUEST_FILE:")) {
                    // Handle file sending
                    String requestedFile = clientMessage.substring(13);
                    File file = new File(requestedFile);

                    if (file.exists() && file.isFile()) {
                        System.out.println("Sending file: " + requestedFile);

                        // Send file data
                        FileInputStream fis = new FileInputStream(file);
                        byte[] sendBuffer = new byte[MAXLINE];
                        int bytesRead;
                        while ((bytesRead = fis.read(sendBuffer)) != -1) {
                            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, bytesRead, clientAddress,
                                    clientPort);
                            socket.send(sendPacket);
                        }
                        fis.close();

                        // Notify client of file transfer completion
                        byte[] doneMessage = "FILE_DONE".getBytes();
                        DatagramPacket donePacket = new DatagramPacket(doneMessage, doneMessage.length, clientAddress,
                                clientPort);
                        socket.send(donePacket);
                        System.out.println("File sent successfully.");
                    } else {
                        // Notify client that file was not found
                        String errorMessage = "ERROR: File not found";
                        DatagramPacket errorPacket = new DatagramPacket(errorMessage.getBytes(), errorMessage.length(),
                                clientAddress, clientPort);
                        socket.send(errorPacket);
                        System.out.println("File not found: " + requestedFile);
                    }

                } else if ("DISCONNECT".equals(clientMessage)) {
                    // Handle client disconnect
                    System.out.println("Client disconnected.");
                    break;
                } else {
                    // Respond to other messages
                    String response = "Server received: " + clientMessage;
                    byte[] sendBuffer = response.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress,
                            clientPort);
                    socket.send(sendPacket);
                    System.out.println("Response sent to client.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Server socket closed.");
            }
        }
    }
}
