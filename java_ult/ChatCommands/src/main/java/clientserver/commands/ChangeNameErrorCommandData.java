package clientserver.commands;

import java.io.Serializable;

public class ChangeNameErrorCommandData implements Serializable {

    private final String errorMessage;

    public ChangeNameErrorCommandData(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
