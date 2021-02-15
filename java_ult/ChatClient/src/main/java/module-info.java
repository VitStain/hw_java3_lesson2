module ChatClient {
    requires javafx.controls;
    requires javafx.fxml;
    requires ChatCommands;

    opens client.controllers to javafx.fxml;
    exports client;
}