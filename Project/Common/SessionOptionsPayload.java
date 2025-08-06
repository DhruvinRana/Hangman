package Project.Common;

public class SessionOptionsPayload extends Payload {
    private boolean removeStrikeOnCorrectGuess;
    private boolean hardMode;

    public SessionOptionsPayload() {
        setPayloadType(PayloadType.SESSION_OPTIONS);
    }

    public boolean isRemoveStrikeOnCorrectGuess() {
        return removeStrikeOnCorrectGuess;
    }

    public void setRemoveStrikeOnCorrectGuess(boolean removeStrikeOnCorrectGuess) {
        this.removeStrikeOnCorrectGuess = removeStrikeOnCorrectGuess;
    }

    public boolean isHardMode() {
        return hardMode;
    }

    public void setHardMode(boolean hardMode) {
        this.hardMode = hardMode;
    }
} 