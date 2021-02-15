package clientserver.commands;

import java.io.Serializable;

public class ChangeNameOkCommandData implements Serializable {

    private final String username;

    public ChangeNameOkCommandData(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
