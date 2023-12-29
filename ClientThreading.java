import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientThreading {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendFileButton;
    private JButton sendImageButton;
    private JButton sendPrivateButton;
    private JButton sendGroupButton;
    private JButton exitButton;
    private JButton sendEmojiButton;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;

    public ClientThreading() {
        frame = new JFrame("Chat Client");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        sendFileButton = new JButton("Send File");
        sendImageButton = new JButton("Send Image");
        sendPrivateButton = new JButton("Send Private Message");
        sendGroupButton = new JButton("Send Group Message");
        exitButton = new JButton("Exit");
        sendEmojiButton = new JButton("Send Emoji");

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });

        sendImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendImage();
            }
        });

        sendPrivateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendPrivateMessage();
            }
        });

        sendGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendGroupMessage();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitChat();
            }
        });

        sendEmojiButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendEmoji();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(sendFileButton);
        buttonPanel.add(sendImageButton);
        buttonPanel.add(sendPrivateButton);
        buttonPanel.add(sendGroupButton);
        buttonPanel.add(exitButton);
        buttonPanel.add(sendEmojiButton);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputField, BorderLayout.SOUTH);
        frame.add(buttonPanel, BorderLayout.PAGE_END);

        frame.setVisible(true);

        connectToServer();
        setupReceiverThread();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 2340);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            chatArea.append("Connected to the server\n");

            // Read welcome message
            String welcomeMessage = dis.readUTF();
            updateChatArea(welcomeMessage);

            // Prompt for username
            username = JOptionPane.showInputDialog("Enter your username:");
            dos.writeUTF(username);

            // Read authentication result
            String authResult = dis.readUTF();
            updateChatArea(authResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupReceiverThread() {
        Thread receiveThread = new Thread(() -> {
            try {
                while (true) {
                    String received = dis.readUTF();
                    updateChatArea(received);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiveThread.start();
    }

    private void sendMessage() {
        try {
            String message = inputField.getText();
            dos.writeUTF("Text");
            dos.writeUTF(message);
            inputField.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());

                dos.writeUTF("File");
                dos.writeUTF(selectedFile.getName());
                dos.writeLong(fileBytes.length);
                dos.write(fileBytes, 0, fileBytes.length);
                dos.flush();

                updateChatArea("File '" + selectedFile.getName() + "' sent.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendImage() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());

                dos.writeUTF("Image");
                dos.writeUTF(selectedFile.getName());
                dos.writeLong(imageBytes.length);
                dos.write(imageBytes, 0, imageBytes.length);
                dos.flush();

                updateChatArea("Image '" + selectedFile.getName() + "' sent.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPrivateMessage() {
        try {
            String recipient = JOptionPane.showInputDialog("Enter the recipient username:");
            String message = JOptionPane.showInputDialog("Enter your private message:");

            dos.writeUTF("Private");
            dos.writeUTF(recipient);
            dos.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendGroupMessage() {
        try {
            String message = JOptionPane.showInputDialog("Enter your group message:");
            dos.writeUTF("GroupMessage");
            dos.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmoji() {
        try {
            String emoji = JOptionPane.showInputDialog("Enter the emoji (e.g., 😊):");

            dos.writeUTF("Emoji");
            dos.writeUTF(emoji);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exitChat() {
        try {
            dos.writeUTF("Exit");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateChatArea(String message) {
        String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> chatArea.append("[" + timeStamp + "] " + message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientThreading::new);
    }
}
