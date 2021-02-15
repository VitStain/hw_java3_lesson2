package client.models;

import clientserver.Command;
import clientserver.commands.*;
import client.ChatNetworkClient;
import client.controllers.ChatController;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Network {

    private static final String SERVER_ADRESS = "localhost";
    private static final int SERVER_PORT = 8828;

    private final String host;
    private final int port;

    private ObjectOutputStream dataOutputStream;
    private ObjectInputStream dataInputStream;

    private Socket socket;

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;

    public static List<String> userList = new ArrayList<>();

    public ObjectOutputStream getDataOutputStream() {
        return dataOutputStream;
    }

    public ObjectInputStream getDataInputStream() {
        return dataInputStream;
    }

    public Network() {
        this(SERVER_ADRESS, SERVER_PORT);
    }

    public Network(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            dataOutputStream = new ObjectOutputStream(socket.getOutputStream());
            dataInputStream = new ObjectInputStream(socket.getInputStream());

            return true;

        } catch (IOException e) {
            System.out.println("Соединение не было установлено!");
            e.printStackTrace();
            return false;
        }

    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void waitMessage(ChatController chatController) {

        Thread thread = new Thread( () -> {
            try { while (true) {

                Command command = readCommand();
                if(command == null) {
                    ChatNetworkClient.showErrorMessage("Error","Ошибка сервера", "Получена неверная команда");
                    continue;
                }

                switch (command.getType()) {
                    case INFO_MESSAGE: {
                        MessageInfoCommandData data = (MessageInfoCommandData) command.getData();
                        String message = data.getMessage();
                        String sender = data.getSender();
                        String formattedMessage = sender != null ? String.format("%s: %s", sender, message) : message;
                        Platform.runLater(() -> {
                            chatController.appendMessage(formattedMessage);
                        });
                        break;
                    }
                    case ERROR: {
                        ErrorCommandData data = (ErrorCommandData) command.getData();
                        String errorMessage = data.getErrorMessage();
                        Platform.runLater(() -> {
                            ChatNetworkClient.showErrorMessage("Error", "Server error", errorMessage);
                        });
                        break;
                    }
                    case CHANGENAME_OK: {
                        ChangeNameOkCommandData data = (ChangeNameOkCommandData) command.getData();
                        this.username = data.getUsername();

                        break;
                    }

                    case UPDATE_USERS_LIST: {
                        List<String> data = (List<String>) command.getData();
                        userList.clear();
                        userList.addAll(data);
                        Platform.runLater(chatController::newUserList);
                        break;
                    }
                    case CHANGENAME_ERROR: {
                        ChangeNameErrorCommandData data = (ChangeNameErrorCommandData) command.getData();
                        Platform.runLater(() -> {
                            ChatNetworkClient.showErrorMessage("Ошибка смены имени!", data.getErrorMessage(), "Повторите ввод.");

                        });
                        break;
                    }
                    default: {
                        Platform.runLater(() -> {
                            ChatNetworkClient.showErrorMessage("Error", "Unknown command from server!", command.getType().toString());
                        });
                    }
                }

            }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Соединение потеряно!");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


    public String sendAuthCommand(String login, String password) {
        try {
            Command authCommand = Command.authCommand(login, password);
            dataOutputStream.writeObject(authCommand);

            Command command = readCommand();
            if (command == null) {
                return "Ошибка чтения команды с сервера";
            }

            switch (command.getType()) {
                case AUTH_OK: {
                    AuthOkCommandData data = (AuthOkCommandData) command.getData();
                    this.username = data.getUsername();
                    return null;
                }

                case AUTH_ERROR:{
                    AuthErrorCommandData data = (AuthErrorCommandData) command.getData();
                    return data.getErrorMessage();
                }
                case ERROR: {
                    ErrorCommandData data = (ErrorCommandData) command.getData();
                    return data.getErrorMessage();
                }

                default:
                    return "Unknown type of command: " + command.getType();

            }
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    public String getUsername() {
        return username;
    }


    public void sendMessage(String message) throws IOException {
        sendMessage(Command.publicMessageCommand(username, message));
    }

    public void sendMessage(Command command) throws IOException {
        dataOutputStream.writeObject(command);
    }



    public void sendPrivateMessage(String message, String recipient) throws IOException {
        Command command = Command.privateMessageCommand(recipient, message);
        sendMessage(command);
    }

    public Command readCommand() throws IOException {
        try {
            return (Command) dataInputStream.readObject();
        } catch (ClassNotFoundException e) {
            String errorMessage = "Получен неизвестный объект";
            System.err.println(errorMessage);
            e.printStackTrace();
            sendMessage(Command.errorCommand(errorMessage));
            return null;
        }
    }

    public void sendExitMessage(){
        Command command = Command.endCommand();
        try {
            sendMessage(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendChangeNameCommand(String lastUsername, String username) {
        try {
            Command changeNameCommand = Command.changeNameCommand(lastUsername, username);
            dataOutputStream.writeObject(changeNameCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

