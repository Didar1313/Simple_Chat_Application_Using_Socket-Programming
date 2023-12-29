import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class ServerSwing {
    static final Set<ClientHandler> clients = new HashSet<>();

    public static void main(String[] args) throws Exception {
        createDirectories(); // Create necessary directories
        ServerSocket ss = new ServerSocket(2340);
        System.out.println("Waiting for clients");

        while (true) {
            Socket s = ss.accept();
            System.out.println("A new client is connected " + s);

            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            DataInputStream dis = new DataInputStream(s.getInputStream());

            dos.writeUTF("Welcome to the chat! Please enter your username:");
            String username = dis.readUTF();
            dos.writeUTF("Hello, " + username + "! You have joined the chat.");

            ClientHandler clientHandler = new ClientHandler(s, dis, dos, username);
            clients.add(clientHandler);

            Thread t = new Thread(clientHandler);
            t.start();
        }
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get("server_received_files"));
            Files.createDirectories(Paths.get("server_received_images"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                try {
                    client.output.writeUTF(message);
                } catch (IOException e) {
                    System.out.println("Error broadcasting message to " + client.username + ": " + e.getMessage());
                }
            }
        }
    }

    static void broadcastGroupMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            try {
                client.output.writeUTF(message);
            } catch (IOException e) {
                System.out.println("Error broadcasting group message to " + client.username + ": " + e.getMessage());
            }
        }
    }

    static void sendPrivateMessage(String message, String recipient, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client.username.equals(recipient)) {
                try {
                    client.output.writeUTF(sender.username + " (private): " + message);
                } catch (IOException e) {
                    System.out.println("Error sending private message to " + client.username + ": " + e.getMessage());
                }
                break;
            }
        }
    }
}

class ClientHandler implements Runnable {
    final Socket soc;
    final DataInputStream input;
    final DataOutputStream output;
    final String username;

    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos, String username) {
        this.soc = s;
        this.input = dis;
        this.output = dos;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            while (true) {
                output.writeUTF("What do you want? [Text/Emoji/File/Image/Private/GroupMessage/Exit]");
                String choice = input.readUTF();

                switch (choice) {
                    case "Text":
                        sendMessage();
                        break;
                    case "Emoji":
                        sendEmoji();
                        break;
                    case "File":
                        receiveFile();
                        break;
                    case "Image":
                        receiveImage();
                        break;
                    case "Private":
                        sendPrivateMessage();
                        break;
                    case "GroupMessage":
                        sendGroupMessage();
                        break;
                    case "Exit":
                        exitChat();
                        break;
                    default:
                        output.writeUTF("Invalid input");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error in ClientHandler: " + e.getMessage());
        } finally {
            try {
                ServerSwing.clients.remove(this);
                this.soc.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void sendMessage() throws IOException {
        output.writeUTF("Enter your message:");
        String message = input.readUTF();
        ServerSwing.broadcastMessage(username + ": " + message, this);
    }

    private void sendEmoji() throws IOException {
        output.writeUTF("Enter the emoji (e.g., 😊):");
        String emoji = input.readUTF();
        ServerSwing.broadcastMessage(username + ": " + emoji, this);
    }

    private void sendPrivateMessage() throws IOException {
        output.writeUTF("Enter the recipient username:");
        String recipient = input.readUTF();
        output.writeUTF("Enter your private message:");
        String message = input.readUTF();
        ServerSwing.sendPrivateMessage(username + " (private): " + message, recipient, this);
    }

    private void sendGroupMessage() throws IOException {
        output.writeUTF("Enter your group message:");
        String message = input.readUTF();
        ServerSwing.broadcastGroupMessage(username + " (group): " + message, this);
    }

    private void receiveFile() {
        try {
            String fileName = input.readUTF();
            long fileSize = input.readLong();

            byte[] fileBytes = new byte[(int) fileSize];
            input.readFully(fileBytes, 0, fileBytes.length);

            // Save the received file to the server's file system
            String filePath = "server_received_files/" + fileName;
            Files.write(Paths.get(filePath), fileBytes);

            // Broadcast the information about the received file
            String broadcastMessage = "File received: " + fileName;
            ServerSwing.broadcastMessage(broadcastMessage, this);
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    private void receiveImage() {
        try {
            String imageName = input.readUTF();
            long imageSize = input.readLong();

            byte[] imageBytes = new byte[(int) imageSize];
            input.readFully(imageBytes, 0, imageBytes.length);

            // Save the received image to the server's file system
            String imagePath = "server_received_images/" + imageName;
            Files.write(Paths.get(imagePath), imageBytes);

            // Broadcast the information about the received image
            String broadcastMessage = "Image received: " + imageName;
            ServerSwing.broadcastMessage(broadcastMessage, this);
        } catch (IOException e) {
            System.out.println("Error receiving image: " + e.getMessage());
        }
    }

    private void exitChat() {
        try {
            ServerSwing.broadcastMessage("User " + username + " has left the chat.", this);
            ServerSwing.clients.remove(this);
            soc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
