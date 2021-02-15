package clientserver.commands;

import java.io.Serializable;

public class ChangeNameCommandData implements Serializable {

    private final String lastUsername;
    private final String username;

    public ChangeNameCommandData(String lastUsername, String username) {
        this.lastUsername = lastUsername;
        this.username = username;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    public String getUsername() {
        return username;
    }
}
