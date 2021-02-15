package client.controllers;

import client.ChatNetworkClient;
import client.models.Network;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatController {


    @FXML
    public ListView<String> usersList;

    @FXML
    private Button sendButton;
    @FXML
    private TextArea chatHistory;
    @FXML
    private TextField textField;
    @FXML
    private TextField changeNameField;
    @FXML
    private Label usernameTitle;
    @FXML
    private ChoiceBox<String> userSend;
    @FXML
    private Hyperlink change;

    private Network network;

    private List <String> user = new ArrayList<>();



    public void setLabel(String usernameTitle) {
        this.usernameTitle.setText(usernameTitle);
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @FXML
    public void initialize() {
        user.add(0,"Всем");
        user.addAll(Network.userList);
        user.remove(usernameTitle.getText());
        userSend.setItems(FXCollections.observableArrayList(user));
        userSend.setValue(user.get(0));
        usersList.setItems(FXCollections.observableArrayList(Network.userList));
        sendButton.setOnAction(event -> ChatController.this.sendMessage());
        textField.setOnAction(event -> ChatController.this.sendMessage());


    }

    private void sendMessage() {
        String message = textField.getText();

        if(message.isBlank()) {
            return;
        }

        appendMessage("Я: " + message);
        textField.clear();

        try {
            if (userSend.getValue().equals("Всем")) {
                network.sendMessage(message);
            } else {
                network.sendPrivateMessage(message, userSend.getValue());
            }

        } catch (IOException e) {
            e.printStackTrace();
            ChatNetworkClient.showErrorMessage("Ошибка подключения", "Ошибка при отправке сообщения", e.getMessage());
        }

    }

    public void appendMessage(String message) {
        String timestamp = DateFormat.getInstance().format(new Date());
        chatHistory.appendText(timestamp);
        chatHistory.appendText(System.lineSeparator());
        chatHistory.appendText(message);
        chatHistory.appendText(System.lineSeparator());
        chatHistory.appendText(System.lineSeparator());
    }


    public void newUserList(){
        user.clear();
        user.add(0,"Всем");
        user.addAll(Network.userList);
        usernameTitle.setText(network.getUsername());
        user.remove(usernameTitle.getText());
        userSend.setItems(FXCollections.observableArrayList(user));
        userSend.setValue(user.get(0));
        usersList.setItems(FXCollections.observableArrayList(Network.userList));
    }

    public void changeUsernameField() {
        change.setVisible(false);
        changeNameField.setVisible(true);
        changeNameField.setText(usernameTitle.getText());
    }

    public void changeName() {
        String lastUsername = network.getUsername();
        String username = changeNameField.getText();

        if (username.isBlank()) {
            ChatNetworkClient.showErrorMessage("Ошибка смены имени", "Ошибка ввода", "Поле не должно быть пустое");
            return;
        }
        if (username.equals(lastUsername)) {
            ChatNetworkClient.showErrorMessage("Ошибка смены имени", "Ошибка ввода", "Вы вводите старое имя");
            return;
        }

        network.sendChangeNameCommand(lastUsername, username);
        change.setVisible(true);
        changeNameField.setVisible(false);


    }


}
