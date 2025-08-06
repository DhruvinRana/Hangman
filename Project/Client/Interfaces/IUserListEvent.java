package Project.Client.Interfaces;

public interface IUserListEvent extends IClientEvents {
    void onUserAwayStatus(long clientId, boolean isAway);
} 