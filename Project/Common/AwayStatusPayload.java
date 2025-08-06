package Project.Common;

public class AwayStatusPayload extends Payload {
    private boolean away;
    private long clientId;
    private String clientName;

    public AwayStatusPayload() {
        setPayloadType(PayloadType.AWAY_STATUS);
    }

    public boolean isAway() {
        return away;
    }

    public void setAway(boolean away) {
        this.away = away;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
} 