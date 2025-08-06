package Project.Client.Interfaces;

public interface IGameStateEvent {
    void onWordUpdate(String wordDisplay);
    void onStrikesUpdate(int strikes);
} 