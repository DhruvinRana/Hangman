package Project.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;
import Project.Common.TimerType;
import Project.Exceptions.MissingCurrentPlayerException;
import Project.Exceptions.NotPlayersTurnException;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PlayerNotFoundException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;


public class GameRoom extends BaseGameRoom {

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;
    private List<ServerThread> turnOrder = new ArrayList<>();
    private long currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
    private int round = 0;
    
    // Word management
    private List<String> wordList = new ArrayList<>();
    private String currentWord;
    private Set<Character> guessedLetters = new HashSet<>();
    private int strikes = 0;
    private static final int MAX_STRIKES = 6;
    private static final int MAX_ROUNDS = 5;
    private static final String WORDS_FILE = "Project/words.txt";

    // Session options
    private boolean removeStrikeOnCorrectGuess = false;
    private boolean hardMode = false;

    private Set<Long> awayPlayers = new HashSet<>();
    private Set<Long> spectators = new HashSet<>();


    public GameRoom(String name) {
        super(name);
        loadWordList();
    }


    private void loadWordList() {
        try (Scanner scanner = new Scanner(new File(WORDS_FILE))) {
            while (scanner.hasNextLine()) {
                String word = scanner.nextLine().trim().toUpperCase();
                if (!word.isEmpty()) {
                    wordList.add(word);
                }
            }
            LoggerUtil.INSTANCE.info("Loaded " + wordList.size() + " words");
        } catch (FileNotFoundException e) {
            LoggerUtil.INSTANCE.severe("Could not load words file: " + e.getMessage());
        }
    }

    private String selectRandomWord() {
        if (wordList.isEmpty()) {
            loadWordList(); // Reload if empty
        }
        int index = new Random().nextInt(wordList.size());
        String word = wordList.get(index);
        wordList.remove(index); // Remove used word
        return word;
    }

    private String getWordDisplay() {
        StringBuilder display = new StringBuilder();
        for (char c : currentWord.toCharArray()) {
            if (guessedLetters.contains(c)) {
                display.append(c);
            } else {
                display.append('_');
            }
            display.append(' ');
        }
        return display.toString().trim();
    }

    private boolean isWordComplete() {
        for (char c : currentWord.toCharArray()) {
            if (!guessedLetters.contains(c)) {
                return false;
            }
        }
        return true;
    }

    private int calculateLetterPoints(char letter) {
        int count = 0;
        for (char c : currentWord.toCharArray()) {
            if (c == letter && !guessedLetters.contains(c)) {
                count++;
            }
        }
        return count;
    }

    private int calculateWordPoints() {
        int emptySpaces = 0;
        for (char c : currentWord.toCharArray()) {
            if (!guessedLetters.contains(c)) {
                emptySpaces++;
            }
        }
        return emptySpaces * 2; // Bonus points for solving
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerThread sp) {
        // sync GameRoom state to new client
        syncCurrentPhase(sp);
        syncReadyStatus(sp);
        syncTurnStatus(sp);
        syncPlayerPoints(sp);

        if (sp.user != null && sp.user.isSpectator()) {
            addSpectator(sp.getClientId());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerThread sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        long removedClient = sp.getClientId();
        turnOrder.removeIf(player -> player.getClientId() == sp.getClientId());
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        } else if (removedClient == currentTurnClientId) {
            onTurnStart();
        }
    }

    // timer handlers
    @SuppressWarnings("unused")
    private void startRoundTimer() {
        roundTimer = new TimedEvent(30, () -> onRoundEnd());
        roundTimer.setTickCallback((time) -> {
            System.out.println("Round Time: " + time);
            sendCurrentTime(TimerType.ROUND, time);
        });
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
            sendCurrentTime(TimerType.ROUND, -1);
        }
    }

    private void startTurnTimer() {
        turnTimer = new TimedEvent(30, () -> onTurnEnd());
        turnTimer.setTickCallback((time) -> {
            System.out.println("Turn Time: " + time);
            sendCurrentTime(TimerType.TURN, time);
        });
    }

    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
            sendCurrentTime(TimerType.TURN, -1);
        }
    }
    // end timer handlers

    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.IN_PROGRESS);
        currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
        setTurnOrder();
        round = 0;

        strikes = 0;
        guessedLetters.clear();
        currentWord = selectRandomWord();
        sendGameEvent("STRIKES:0");
        sendGameEvent("New game started! Word: " + getWordDisplay());
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        round++;

        strikes = 0;
        guessedLetters.clear();
        currentWord = selectRandomWord();
        sendGameEvent("STRIKES:0");
        sendGameEvent("Round " + round + " started! Word: " + getWordDisplay());
        resetRoundTimer();
        resetTurnStatus();
        LoggerUtil.INSTANCE.info("onRoundStart() end");
        onTurnStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onTurnStart() {
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
        ServerThread currentPlayer = null;
        try {
            currentPlayer = getNextPlayer();
            currentTurnClientId = currentPlayer.getClientId();

            relay(null, String.format("It's %s's turn", currentPlayer.getDisplayName()));
            currentPlayer.sendTurnStatus(currentPlayer.getClientId(), true);
        } catch (MissingCurrentPlayerException | PlayerNotFoundException e) {
            e.printStackTrace();
        }

        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring

        // Classic Hangman: always pass to next player, do not end round after all took a turn
        onTurnStart();
        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }

    // Note: logic between Round Start and Round End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer();
        
        // Display scoreboard
        StringBuilder scoreboard = new StringBuilder("\nScoreboard:\n");
        clientsInRoom.values().stream()
            .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
            .forEach(player -> scoreboard.append(String.format("%s: %d points\n", 
                player.getDisplayName(), player.getPoints())));
        sendGameEvent(scoreboard.toString());

        if (round >= MAX_ROUNDS) {
            onSessionEnd();
        } else {
            onRoundStart();
        }
        LoggerUtil.INSTANCE.info("onRoundEnd() end");

    }

    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        turnOrder.clear();
        currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
        resetTurnTimer();
        resetRoundTimer();
        resetTurnStatus();
        resetReadyStatus();
        sendGameEvent("Game Over! Final scores:\n" + getScoreboard());
        clientsInRoom.values().forEach(s -> s.setPoints(0));

        changePhase(Phase.READY);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
    }

    private String getScoreboard() {
        StringBuilder scoreboard = new StringBuilder();
        clientsInRoom.values().stream()
            .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
            .forEach(player -> scoreboard.append(String.format("%s: %d points\n", 
                player.getDisplayName(), player.getPoints())));
        return scoreboard.toString();
    }
    // end lifecycle methods

    // send/sync data to ServerUser(s)
    private void syncPlayerPoints(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendPlayerPoints(serverUser.getClientId(),
                        serverUser.getPoints());
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    private void sendPlayerPoints(ServerThread sp) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendPlayerPoints(sp.getClientId(), sp.getPoints());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void sendGameEvent(String str) {
        sendGameEvent(str, null);
    }

    private void sendGameEvent(String str, List<Long> targets) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean canSend = false;
            if (targets != null) {
                if (targets.contains(spInRoom.getClientId())) {
                    canSend = true;
                }
            } else {
                canSend = true;
            }
            if (canSend) {
                boolean failedToSend = !spInRoom.sendGameEvent(str);
                if (failedToSend) {
                    removeClient(spInRoom);
                }
                return failedToSend;
            }
            return false;
        });
    }

    private void sendResetTurnStatus() {
        clientsInRoom.values().forEach(spInRoom -> {
            boolean failedToSend = !spInRoom.sendResetTurnStatus();
            if (failedToSend) {
                removeClient(spInRoom);
            }
        });
    }

    private void sendTurnStatus(ServerThread client, boolean tookTurn) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendTurnStatus(client.getClientId(), client.didTakeTurn());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void syncTurnStatus(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendTurnStatus(serverUser.getClientId(),
                        serverUser.didTakeTurn(), true);
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    // end send data to ServerThread(s)

    // misc methods
    private void resetTurnStatus() {
        clientsInRoom.values().forEach(sp -> {
            sp.setTookTurn(false);
        });
        sendResetTurnStatus();
    }

    private void setTurnOrder() {
        turnOrder.clear();
        turnOrder = clientsInRoom.values().stream()
            .filter(sp -> sp.isReady() && !isPlayerAway(sp.getClientId()) && !isSpectator(sp.getClientId()))
            .collect(Collectors.toList());
        Collections.shuffle(turnOrder);
    }

    private ServerThread getCurrentPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        // quick early exit
        if (currentTurnClientId == Constants.DEFAULT_CLIENT_ID) {
            throw new MissingCurrentPlayerException("Current Plaer not set");
        }
        return turnOrder.stream()
                .filter(sp -> sp.getClientId() == currentTurnClientId)
                .findFirst()
                // this shouldn't occur but is included as a "just in case"
                .orElseThrow(() -> new PlayerNotFoundException("Current player not found in turn order"));
    }

    private ServerThread getNextPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        int index = 0;
        if (currentTurnClientId != Constants.DEFAULT_CLIENT_ID) {
            index = turnOrder.indexOf(getCurrentPlayer()) + 1;
            if (index >= turnOrder.size()) {
                index = 0;
            }
        }
        for (int i = 0; i < turnOrder.size(); i++) {
            ServerThread candidate = turnOrder.get((index + i) % turnOrder.size());
            if (!isPlayerAway(candidate.getClientId()) && !isSpectator(candidate.getClientId())) {
                return candidate;
            }
        }
        throw new PlayerNotFoundException("No available player found");
    }

    private boolean isLastPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        // check if the current player is the last player in the turn order
        return turnOrder.indexOf(getCurrentPlayer()) == (turnOrder.size() - 1);
    }

    @SuppressWarnings("unused")
    private void checkAllTookTurn() {
        int numReady = clientsInRoom.values().stream()
                .filter(sp -> sp.isReady())
                .toList().size();
        int numTookTurn = clientsInRoom.values().stream()
                // ensure to verify the isReady part since it's against the original list
                .filter(sp -> sp.isReady() && sp.didTakeTurn())
                .toList().size();
        if (numReady == numTookTurn) {
            relay(null,
                    String.format("All players have taken their turn (%d/%d) ending the round", numTookTurn, numReady));
            onRoundEnd();
        }
    }

    // start check methods
    private void checkCurrentPlayer(long clientId) throws NotPlayersTurnException {
        if (currentTurnClientId != clientId) {
            throw new NotPlayersTurnException("You are not the current player");
        }
    }

    // end check methods

    /**
     * Example turn action
     * 
     * @param currentUser
     */
    protected void handleTurnAction(ServerThread currentUser, String command) {
        try {
            checkPlayerInRoom(currentUser);
            checkCurrentPhase(currentUser, Phase.IN_PROGRESS);
            checkCurrentPlayer(currentUser.getClientId());
            checkIsReady(currentUser);
            
            String[] parts = command.trim().split("\\s+", 2);
            String action = parts[0].toLowerCase();
            String input = (parts.length > 1) ? parts[1].toUpperCase() : "";

            boolean roundShouldEnd = false;
            boolean validAction = false;

            switch (action) {
                case "guess":
                    if (input.isEmpty()) {
                        currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid command format. Use /guess <word>");
                        return;
                    }
                    roundShouldEnd = handleGuess(currentUser, input);
                    validAction = true;
                    break;
                case "letter":
                    if (input.length() != 1 || !Character.isLetter(input.charAt(0))) {
                        currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "Please enter a single letter");
                        return;
                    }
                    roundShouldEnd = handleLetter(currentUser, input.charAt(0));
                    validAction = true;
                    break;
                case "skip":
                    handleSkip(currentUser);
                    validAction = true;
                    break;
                default:
                    currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid command. Use /guess <word>, /letter <letter>, or /skip");
                    return;
            }

            if (validAction) {
                currentUser.setTookTurn(true);
                relay(null, String.format("%s finished their turn", currentUser.getDisplayName()));
            }

            if (roundShouldEnd) {
                onRoundEnd();
            } else {
                onTurnEnd();
            }


        } catch (NotPlayersTurnException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "It's not your turn");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (NotReadyException e) {
            // The check method already informs the currentUser

            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PlayerNotFoundException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do the ready check");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (Exception e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "An error occurred during your turn");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        }
    }


    private boolean handleGuess(ServerThread currentUser, String guess) {
        if (guess.equals(currentWord)) {
            int points = calculateWordPoints();
            currentUser.changePoints(points);
            sendPlayerPoints(currentUser);
            
            // Remove a strike if enabled and there are strikes to remove
            if (removeStrikeOnCorrectGuess && strikes > 0) {
                strikes--;
                sendGameEvent("STRIKES:" + strikes);
                sendGameEvent(String.format("%s removed a strike by guessing the word correctly!", 
                    currentUser.getDisplayName()));
            }
            
            sendGameEvent(String.format("%s guessed the correct word '%s' and got %d points!", 
                currentUser.getDisplayName(), currentWord, points));
            return true; // End round
        } else {
            strikes++;
            sendGameEvent("STRIKES:" + strikes); // Notify clients of new strike count
            sendGameEvent(String.format("%s guessed '%s' and it was wrong", 
                currentUser.getDisplayName(), guess));
            if (strikes >= MAX_STRIKES) {
                sendGameEvent(String.format("Maximum strikes reached! The word was '%s'", currentWord));
                return true; // End round
            }
        }
        return false; // Continue round
    }

    private boolean handleLetter(ServerThread currentUser, char letter) {
        letter = Character.toUpperCase(letter); // Ensure uppercase for comparison

        // In hard mode, allow repeated guesses (do not block)
        if (!hardMode && guessedLetters.contains(letter)) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "That letter has already been guessed");
            return false;
        }

        int matches = 0;
        for (char c : currentWord.toCharArray()) {
            if (c == letter) {
                matches++;
            }
        }

        guessedLetters.add(letter);
        // Only send LETTER_GUESSED if not in hard mode
        if (!hardMode) {
            sendGameEvent("LETTER_GUESSED:" + letter); // Sync to all clients
        }

        if (matches > 0) {
            int points = matches;
            currentUser.changePoints(points);
            sendPlayerPoints(currentUser);
            sendGameEvent(String.format("%s guessed '%c' and there were %d %c's which yielded %d points", 
                currentUser.getDisplayName(), letter, matches, letter, points));
            sendGameEvent("Current word: " + getWordDisplay());
            
            // Remove a strike if enabled and there are strikes to remove
            if (removeStrikeOnCorrectGuess && strikes > 0) {
                strikes--;
                sendGameEvent("STRIKES:" + strikes);
                sendGameEvent(String.format("%s removed a strike by guessing correctly!", 
                    currentUser.getDisplayName()));
            }
            
            if (isWordComplete()) {
                sendGameEvent(String.format("Word completed! The word was '%s'", currentWord));
                return true; // End round
            }
        } else {
            strikes++;
            sendGameEvent("STRIKES:" + strikes); // Notify clients of new strike count
            sendGameEvent(String.format("%s guessed '%c', which isn't in the word", 
                currentUser.getDisplayName(), letter));
            if (strikes >= MAX_STRIKES) {
                sendGameEvent(String.format("Maximum strikes reached! The word was '%s'", currentWord));
                return true; // End round
            }
        }
        return false; // Continue round
    }

    private void handleSkip(ServerThread currentUser) {
        sendGameEvent(String.format("%s skipped their turn", currentUser.getDisplayName()));
    }

    // end receive data from ServerThread (GameRoom specific)

    public void handleSessionOptions(ServerThread sender, Project.Common.SessionOptionsPayload payload) {
        this.removeStrikeOnCorrectGuess = payload.isRemoveStrikeOnCorrectGuess();
        this.hardMode = payload.isHardMode();
        LoggerUtil.INSTANCE.info(String.format("Session options updated: removeStrikeOnCorrectGuess=%b, hardMode=%b", removeStrikeOnCorrectGuess, hardMode));
    }

    // --- Away status logic ---
    public void setPlayerAway(long clientId, boolean away) {
        if (away) {
            awayPlayers.add(clientId);
        } else {
            awayPlayers.remove(clientId);
        }
    }
    public boolean isPlayerAway(long clientId) {
        return awayPlayers.contains(clientId);
    }

    public void addSpectator(long clientId) {
        if (spectators.contains(clientId)) return; // Prevent duplicate adds
        spectators.add(clientId);
        relay(null, getClientNameByIdSafe(clientId) + " joined as a spectator");
    }
    public boolean isSpectator(long clientId) {
        return spectators.contains(clientId);
    }
    private String getClientNameByIdSafe(long clientId) {
        ServerThread st = clientsInRoom.get(clientId);
        return st != null ? st.getClientName() : ("Client " + clientId);
    }

    // end receive data from ServerThread (GameRoom specific)
}
