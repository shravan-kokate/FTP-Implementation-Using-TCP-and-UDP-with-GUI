import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class UDP_ClientGUI {
    // Server details
    private static final String SERVER_IP = "192.168.15.114";
    private static final int PORT = 5000;
    private static final int MAXLINE = 1000;

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private JFrame frame;
    private JTextArea messageArea;
    private JButton sendFileButton, receiveFileButton, disconnectButton;

    public UDP_ClientGUI() {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_IP);

            frame = new JFrame("File Transfer Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 400);

            messageArea = new JTextArea();
            messageArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(messageArea);

            sendFileButton = new JButton("Send File");
            receiveFileButton = new JButton("Receive File");
            disconnectButton = new JButton("Disconnect");

            sendFileButton.addActionListener(e -> sendFile());
            receiveFileButton.addActionListener(e -> receiveFile());
            disconnectButton.addActionListener(e -> disconnect());

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(sendFileButton);
            buttonPanel.add(receiveFileButton);
            buttonPanel.add(disconnectButton);

            frame.setLayout(new BorderLayout());
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.setVisible(true);
        } catch (Exception e) {
            logMessage("Error initializing client: " + e.getMessage());
        }
    }

    private void logMessage(String message) {
        messageArea.append(message + "\n");
    }

    private void sendFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                logMessage("Sending file: " + file.getName());

                String fileName = "FILE:" + file.getName();
                sendMessage(fileName);

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[MAXLINE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    DatagramPacket sendPacket = new DatagramPacket(buffer, bytesRead, serverAddress, PORT);
                    socket.send(sendPacket);
                }
                fis.close();

                sendMessage("FILE_DONE");
                logMessage("File sent successfully.");
            }
        } catch (Exception e) {
            logMessage("Error sending file: " + e.getMessage());
        }
    }

    private void receiveFile() {
        try {
            String fileName = JOptionPane.showInputDialog(frame, "Enter the name of the file to receive:");
            if (fileName == null || fileName.trim().isEmpty()) {
                logMessage("No file name entered.");
                return;
            }

            sendMessage("REQUEST_FILE:" + fileName);
            logMessage("Requesting file: " + fileName);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save File");
            int returnValue = fileChooser.showSaveDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();
                FileOutputStream fos = new FileOutputStream(saveFile);

                byte[] receiveBuffer = new byte[MAXLINE];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                boolean receiving = true;
                while (receiving) {
                    socket.receive(receivePacket);
                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    if ("FILE_DONE".equals(receivedMessage)) {
                        logMessage("File received successfully.");
                        receiving = false;
                    } else {
                        fos.write(receivePacket.getData(), 0, receivePacket.getLength());
                    }
                }
                fos.close();
            }
        } catch (Exception e) {
            logMessage("Error receiving file: " + e.getMessage());
        }
    }

    private void sendMessage(String message) {
        try {
            byte[] sendBuffer = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, PORT);
            socket.send(sendPacket);
        } catch (Exception e) {
            logMessage("Error sending message: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            sendMessage("DISCONNECT");
            logMessage("Disconnected from server.");
            socket.close();
            frame.dispose();
        } catch (Exception e) {
            logMessage("Error disconnecting: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UDP_ClientGUI::new);
    }
}
