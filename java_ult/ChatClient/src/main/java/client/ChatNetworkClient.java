package client;


import client.controllers.AuthController;
import client.controllers.ChatController;
import client.models.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class ChatNetworkClient extends Application {

    private Stage primaryStage;
    private Stage authStage;
    private Network network;
    private ChatController chatController;

    @Override
    public void start(Stage primaryStage) throws Exception {

        this.primaryStage = primaryStage;

        network = new Network();
        if (!network.connect()) {
            showErrorMessage("Проблемы с соединением", "", "Ошибка подключения к серверу");
            return;
        }

        openAuthWindow();
        createMainChatWindow();
    }

    public void openAuthWindow() throws IOException {
        authStage = new Stage();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ChatNetworkClient.class.getResource("auth-view.fxml"));
        Parent root = loader.load();

        authStage.setTitle("Авторизация");
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.initOwner(primaryStage);
        Scene scene = new Scene(root);
        authStage.setScene(scene);
        authStage.show();

        TimerTask timeout = new TimerTask() {
            @Override
            public void run() {
                if (!primaryStage.isShowing()) {
                    network.close();
                    System.exit(2);
                }
            }
        };
        new Timer().schedule(timeout, AuthController.deadlineReg * 1000);

        AuthController authController = loader.getController();
        authController.setNetwork(network);
        authController.setNetworkClient(this);



    }

    public void createMainChatWindow() throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ChatNetworkClient.class.getResource("chat-view.fxml"));

        Parent root = loader.load();

        primaryStage.setTitle("Messenger");
        primaryStage.setScene(new Scene(root/*, 600, 400*/));

        chatController = loader.getController();
        chatController.setNetwork(network);

        primaryStage.setOnCloseRequest(windowEvent -> {
            network.sendExitMessage();
            network.close();
            System.exit(2);
        });
    }


    public static void showErrorMessage(String title, String message, String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void openMainChatWindow() {
        authStage.close();
        primaryStage.show();

        primaryStage.setTitle("ЧАТ!!!");
        primaryStage.setAlwaysOnTop(false);
        chatController.setLabel(network.getUsername());
        network.waitMessage(chatController);
    }


}