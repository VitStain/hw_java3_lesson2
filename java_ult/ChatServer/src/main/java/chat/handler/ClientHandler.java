package chat.handler;

import chat.MyServer;
import chat.auth.AuthService;
import clientserver.Command;
import clientserver.CommandType;
import clientserver.commands.*;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;


public class ClientHandler {

    private final MyServer myServer;
    private final Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet rs;

    private static void connection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\Виталий\\Desktop\\java_ult\\ChatServer\\src\\main\\resources\\db\\main.db");
        stmt = connection.createStatement();
    }

    private static void disconnection() throws SQLException {
        connection.close();
    }

    public ClientHandler(MyServer myServer, Socket clientSocket) {
        this.myServer = myServer;
        this.clientSocket = clientSocket;
    }

    public void handle() throws IOException {
        in = new ObjectInputStream(clientSocket.getInputStream());
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }).start();

    }

    private void authentication() throws IOException {

        while (true) {

            Command command = readCommand();
            if (command == null) {
                continue;
            }
            if (command.getType() == CommandType.AUTH) {

                boolean isSuccessAuth = processAuthCommand(command);

                if (isSuccessAuth) {
                    break;
                }

            } else {
                sendMessage(Command.authErrorCommand("Ошибка действия"));

            }
        }

    }

    private boolean processAuthCommand(Command command) throws IOException {
        AuthCommandData cmdData = (AuthCommandData) command.getData();
        String login = cmdData.getLogin();
        String password = cmdData.getPassword();

        AuthService authService = myServer.getAuthService();
        try {
            this.username = authService.getUsernameByLoginAndPassword(login, password);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (username != null) {
            if (myServer.isUsernameBusy(username)) {
                sendMessage(Command.authErrorCommand("Логин уже используется"));
                return false;
            }

            UpdateUsersListCommandData.users.clear();
            for (ClientHandler client : myServer.getClients()) {
                UpdateUsersListCommandData.users.add(client.getUsername());
            }


            sendMessage(Command.authOkCommand(username));
            String message = String.format(">>> %s присоединился к чату", username);
            myServer.broadcastMessage(this, Command.messageInfoCommand(message, null));
            myServer.subscribe(this);
            return true;
        } else {
            sendMessage(Command.authErrorCommand("Логин или пароль не соответствуют действительности"));
            return false;
        }
    }


    private Command readCommand() throws IOException {
        try {
            return (Command) in.readObject();
        } catch (ClassNotFoundException e) {
            String errorMessage = "Получен неизвестный объект";
            System.err.println(errorMessage);
            e.printStackTrace();
            return null;
        }
    }

    private void readMessage() throws IOException {

        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }

            switch (command.getType()) {
                case END:
                    String messageExit = String.format(">>> %s покинул чат", username);
                    myServer.broadcastMessage(this, Command.messageInfoCommand(messageExit, null));
                    myServer.unSubscribe(this);
                    return;
                case PUBLIC_MESSAGE: {
                    PublicMessageCommandData data = (PublicMessageCommandData) command.getData();
                    String message = data.getMessage();
                    String sender = data.getSender();
                    myServer.broadcastMessage(this, Command.messageInfoCommand(message, sender));
                    break;
                }
                case PRIVATE_MESSAGE: {
                    PrivateMessageCommandData data = (PrivateMessageCommandData) command.getData();
                    String recipient = data.getReceiver();
                    String message = data.getMessage();
                    myServer.sendPrivateMessage(recipient, Command.messageInfoCommand(message, username));
                    break;
                }
                case CHANGE_NAME: {
                    ChangeNameCommandData data = (ChangeNameCommandData) command.getData();
                    String lastUsername = data.getLastUsername();
                    String username = data.getUsername();

                    try {
                        connection();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    try {
                        int result = stmt.executeUpdate(String.format("UPDATE users SET username = '%s' WHERE username = '%s';", username, lastUsername));
                        try {
                            disconnection();
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                        if (result == 1) {


                            myServer.sendPrivateMessage(lastUsername, Command.changeNameOkCommand(username));
                            this.username = username;
                            UpdateUsersListCommandData.users.clear();
                            for (ClientHandler client : myServer.getClients()) {
                                UpdateUsersListCommandData.users.add(client.getUsername());
                            }

                            myServer.broadcastMessage(null, Command.updateUsersListCommand(myServer.getAllUsernames()));
                            String messageChangeName = String.format(">>> %s сменил имя на %s", lastUsername, username);
                            myServer.broadcastMessage(this, Command.messageInfoCommand(messageChangeName, null));


                        } else {
                            sendMessage(Command.changeNameErrorCommand("Логин уже используется"));
                        }

                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }

                }
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(Command command) throws IOException {
        out.writeObject(command);
    }
}
