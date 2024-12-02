import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class TCP_ClientGUI extends JFrame {
    private Socket client;
    private DataInputStream dis;
    private DataOutputStream dos;

    private JTextArea logArea;
    private JButton connectButton, sendButton, receiveButton, disconnectButton;

    // Constructor
    public TCP_ClientGUI() {
        setTitle("File Transfer Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        JPanel buttonPanel = new JPanel();
        connectButton = new JButton("Connect to Server");
        sendButton = new JButton("Send File");
        receiveButton = new JButton("Receive File");
        disconnectButton = new JButton("Disconnect");

        buttonPanel.add(connectButton);
        buttonPanel.add(sendButton);
        buttonPanel.add(receiveButton);
        buttonPanel.add(disconnectButton);

        add(scrollPane, BorderLayout.CENTER);// log area to the center of the window
        add(buttonPanel, BorderLayout.SOUTH);// button to bottom of the window

        connectButton.addActionListener(e -> connectToServer());
        sendButton.addActionListener(e -> sendFile());
        receiveButton.addActionListener(e -> listFilesAndReceive());
        disconnectButton.addActionListener(e -> disconnectFromServer());

        sendButton.setEnabled(false);
        receiveButton.setEnabled(false);
        disconnectButton.setEnabled(false);
    }

    // Methods
    private void connectToServer() {
        try {
            logArea.append("Connecting to server...\n");
            client = new Socket("192.168.15.114", 8888); // add sserver  static (custom ) or  dynamic IP address here.
            dis = new DataInputStream(client.getInputStream());
            dos = new DataOutputStream(client.getOutputStream());
            logArea.append("Connected to server!\n");
            sendButton.setEnabled(true);
            receiveButton.setEnabled(true);
            disconnectButton.setEnabled(true);
        } catch (Exception e) {
            logArea.append("Unable to connect to server.\n");
        }
    }

    private void disconnectFromServer() {
        try {
            client.close();
            logArea.append("Disconnected from server.\n");
            sendButton.setEnabled(false);
            receiveButton.setEnabled(false);
            disconnectButton.setEnabled(false);
        } catch (Exception e) {
            logArea.append("Error disconnecting from server.\n");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                dos.writeUTF("FILE_SEND_FROM_CLIENT");
                dos.writeUTF(file.getName());
                dos.writeInt(data.length);
                dos.write(data);
                logArea.append("File sent successfully: " + file.getName() + "\n");
            } catch (Exception e) {
                logArea.append("Error sending the file.\n");
            }
        }
    }

    private void listFilesAndReceive() {
        try {
            dos.writeUTF("LIST_FILES");// request
            int fileCount = dis.readInt();

            if (fileCount > 0) {
                String[] files = new String[fileCount];
                for (int i = 0; i < fileCount; i++) {
                    files[i] = dis.readUTF();
                }

                String selectedFile = (String) JOptionPane.showInputDialog(
                        this,
                        "Select a file to download:",
                        "Available Files",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        files,
                        files[0]);

                if (selectedFile != null) {
                    dos.writeUTF("DOWNLOAD_FILE");
                    dos.writeUTF(selectedFile);

                    String response = dis.readUTF();
                    if (response.equals("FILE_NOT_FOUND")) {
                        logArea.append("File not found on the server.\n");
                    } else {
                        int fileSize = dis.readInt();
                        byte[] data = new byte[fileSize];
                        dis.readFully(data);

                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setSelectedFile(new File(selectedFile));
                        int result = fileChooser.showSaveDialog(this);

                        if (result == JFileChooser.APPROVE_OPTION) {
                            FileOutputStream fos = new FileOutputStream(fileChooser.getSelectedFile());
                            fos.write(data);
                            fos.close();
                            logArea.append("File received and saved successfully.\n");
                        }
                    }
                }
            } else {
                logArea.append("No files available on the server.\n");
            }
        } catch (Exception e) {
            logArea.append("Error receiving the file.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TCP_ClientGUI clientGUI = new TCP_ClientGUI();
            clientGUI.setVisible(true);
        });
    }
}
