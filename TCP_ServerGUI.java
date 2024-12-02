import java.io.*;
import java.net.*;
import java.util.*;

public class TCP_ServerGUI {
    private static final int PORT = 8888;
    private static final String SERVER_DIRECTORY = "server_files";

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("192.168.15.114"));
            System.out.println("Server started on IP 192.168.15.114, Port 8888."); // Add client side static (custom ) or dynamic ip address here .

            // Create server directory if it doesn't exist
            File directory = new File(SERVER_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdir();
            }

            while (true) {
                System.out.println("Waiting for client connections...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle client in a new thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            while (true) {
                String command = dis.readUTF();

                switch (command) {
                    case "FILE_SEND_FROM_CLIENT":
                        receiveFile(dis);
                        break;

                    case "LIST_FILES":
                        listFiles(dos);
                        break;

                    case "DOWNLOAD_FILE":
                        sendFile(dis, dos);
                        break;

                    default:
                        System.out.println("Unknown command from client: " + command);
                        break;
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected.");
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private static void receiveFile(DataInputStream dis) {
        try {
            String fileName = dis.readUTF();
            int fileSize = dis.readInt();

            byte[] data = new byte[fileSize];
            dis.readFully(data);

            File file = new File(SERVER_DIRECTORY, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }

            System.out.println("File received and saved: " + fileName);
        } catch (Exception e) {
            System.err.println("Error receiving file: " + e.getMessage());
        }
    }

    private static void listFiles(DataOutputStream dos) {
        try {
            File directory = new File(SERVER_DIRECTORY);
            File[] files = directory.listFiles();

            if (files != null && files.length > 0) {
                dos.writeInt(files.length);
                for (File file : files) {
                    dos.writeUTF(file.getName());
                }
            } else {
                dos.writeInt(0); // No files available
            }
        } catch (Exception e) {
            System.err.println("Error listing files: " + e.getMessage());
        }
    }

    private static void sendFile(DataInputStream dis, DataOutputStream dos) {
        try {
            String fileName = dis.readUTF();
            File file = new File(SERVER_DIRECTORY, fileName);

            if (!file.exists()) {
                dos.writeUTF("FILE_NOT_FOUND");
                return;
            }

            dos.writeUTF("FILE_FOUND");
            dos.writeInt((int) file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                dos.write(data);
            }

            System.out.println("File sent to client: " + fileName);
        } catch (Exception e) {
            System.err.println("Error sending file: " + e.getMessage());
        }
    }
}
