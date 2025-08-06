package Project.Client.Views;

import Project.Client.CardView;
import Project.Client.Client;
import Project.Client.Interfaces.ICardControls;
import Project.Client.Interfaces.IGameStateEvent;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Client.Interfaces.ITurnEvent;
import Project.Common.AwayStatusPayload;
import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.SessionOptionsPayload;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class GamePanel extends JPanel implements IRoomEvents, IPhaseEvent, IGameStateEvent, ITurnEvent {


    private JPanel playPanel;
    private CardLayout cardLayout;
    private static final String READY_PANEL = "READY";

    private static final String PLAY_PANEL = "PLAY";
    private JPanel buttonPanel = new JPanel();
    private JLabel wordDisplayLabel = new JLabel("", SwingConstants.CENTER);
    private JLabel strikesLabel = new JLabel("Strikes: 0/6", SwingConstants.CENTER);
    private JTextField guessInput = new JTextField();
    private JTextField letterInput = new JTextField();
    private HangmanPanel hangmanPanel = new HangmanPanel();
    private Map<Character, JButton> letterButtons = new HashMap<>();
    private JPanel letterGridPanel = new JPanel(new GridLayout(2, 13, 2, 2));
    private boolean hardMode = false;
    private boolean isMyTurn = false;

    @SuppressWarnings("unused")
    public GamePanel(ICardControls controls) {
        super(new BorderLayout());

        Project.Client.Client.currentGamePanel = this;

        // Create the game state display
        JPanel gameStatePanel = new JPanel(new GridLayout(2, 1));
        wordDisplayLabel.setFont(new Font("Arial", Font.BOLD, 28));
        strikesLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        gameStatePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        wordDisplayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        strikesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gameStatePanel.add(wordDisplayLabel);
        gameStatePanel.add(strikesLabel);

        // Create the input panel (compact)
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        
        // Word guess
        JPanel wordGuessPanel = new JPanel(new BorderLayout());
        guessInput.setPreferredSize(new Dimension(100, 25));
        JButton guessButton = new JButton("Guess Word");
        guessButton.setPreferredSize(new Dimension(100, 25));
        guessButton.addActionListener(e -> {
            try {
                String guess = guessInput.getText().trim();
                if (!guess.isEmpty()) {
                    Client.INSTANCE.sendDoTurn("guess " + guess);
                    guessInput.setText("");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        wordGuessPanel.add(guessInput, BorderLayout.CENTER);
        wordGuessPanel.add(guessButton, BorderLayout.EAST);

        // Letter guess
        JPanel letterGuessPanel = new JPanel(new BorderLayout());
        letterInput.setPreferredSize(new Dimension(50, 25));
        JButton letterButton = new JButton("Guess Letter");
        letterButton.setPreferredSize(new Dimension(100, 25));
        letterButton.addActionListener(e -> {
            try {
                String letter = letterInput.getText().trim();
                if (!letter.isEmpty()) {
                    Client.INSTANCE.sendDoTurn("letter " + letter);
                    letterInput.setText("");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        letterGuessPanel.add(letterInput, BorderLayout.CENTER);
        letterGuessPanel.add(letterButton, BorderLayout.EAST);

        // Skip turn
        JButton skipButton = new JButton("Skip Turn");
        skipButton.setPreferredSize(new Dimension(120, 30));
        skipButton.addActionListener(e -> {
            try {
                Client.INSTANCE.sendDoTurn("skip");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Add Away toggle button
        JButton awayButton = new JButton("Mark as Away");
        awayButton.setPreferredSize(new Dimension(120, 30));
        awayButton.addActionListener(e -> {
            boolean newAway = !awayButton.getText().equals("Mark as Away");
            try {
                AwayStatusPayload payload = new AwayStatusPayload();
                payload.setAway(!newAway); // Toggle
                payload.setClientId(Client.INSTANCE.getMyClientId());
                payload.setClientName(Client.INSTANCE.getDisplayNameFromId(Client.INSTANCE.getMyClientId()));
                Client.INSTANCE.sendToServer(payload);
                awayButton.setText(newAway ? "Mark as Away" : "Back");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Add Hard Mode toggle
        JCheckBox hardModeBox = new JCheckBox("Hard Mode");
        hardModeBox.setSelected(Client.hardMode); // reflect current state
        hardModeBox.addActionListener(e -> {
            boolean newHardMode = hardModeBox.isSelected();
            try {
                SessionOptionsPayload payload = new SessionOptionsPayload();
                payload.setHardMode(newHardMode);
                // For now, always send false for removeStrikeOnCorrectGuess (or you can store the current value)
                payload.setRemoveStrikeOnCorrectGuess(false);
                Client.INSTANCE.sendToServer(payload);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        inputPanel.add(wordGuessPanel);
        inputPanel.add(letterGuessPanel);
        inputPanel.add(skipButton);
        inputPanel.add(awayButton);
        inputPanel.add(hardModeBox);

        // Letter grid (A-Z)
        for (char c = 'A'; c <= 'Z'; c++) {
            JButton btn = new JButton(String.valueOf(c));
            btn.setPreferredSize(new Dimension(32, 32));
            btn.addActionListener(e -> {
                try {
                    Client.INSTANCE.sendDoTurn("letter " + btn.getText());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            letterButtons.put(c, btn);
            letterGridPanel.add(btn);
        }
        letterGridPanel.setBorder(BorderFactory.createTitledBorder("Letter Grid"));

        // Add components to button panel
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(gameStatePanel, BorderLayout.NORTH);
        buttonPanel.add(inputPanel, BorderLayout.CENTER);
        buttonPanel.add(letterGridPanel, BorderLayout.SOUTH);

        // Add HangmanPanel to the left of the buttonPanel
        JPanel playArea = new JPanel(new BorderLayout());
        hangmanPanel.setPreferredSize(new Dimension(140, 200));
        playArea.add(hangmanPanel, BorderLayout.WEST);
        playArea.add(buttonPanel, BorderLayout.CENTER);


        // Create the buttons and add them to a panel
        JButton doSomething = new JButton("Do Something");
        doSomething.addActionListener(event -> {
            try {
                Client.INSTANCE.sendDoTurn("example");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        buttonPanel.add(doSomething);


        JPanel gameContainer = new JPanel(new CardLayout());
        cardLayout = (CardLayout) gameContainer.getLayout();
        this.setName(CardView.GAME_SCREEN.name());
        Client.INSTANCE.addCallback(this);

        ReadyPanel readyPanel = new ReadyPanel();
        readyPanel.setName(READY_PANEL);
        gameContainer.add(READY_PANEL, readyPanel);

        playPanel = new JPanel(new BorderLayout());
        playPanel.setName(PLAY_PANEL);
        playPanel.add(playArea, BorderLayout.CENTER);

        gameContainer.add(PLAY_PANEL, playPanel);

        GameEventsPanel gameEventsPanel = new GameEventsPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gameContainer, gameEventsPanel);
        splitPane.setResizeWeight(0.7);

        playPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.7);
            }
        });

        playPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                playPanel.revalidate();
                playPanel.repaint();
            }
        });

        this.add(splitPane, BorderLayout.CENTER);
        controls.addPanel(CardView.CHAT_GAME_SCREEN.name(), this);
        setVisible(false);

        // Add Spectator button
        JButton spectateButton = new JButton("Join as Spectator");
        spectateButton.setPreferredSize(new Dimension(160, 30));
        spectateButton.addActionListener(e -> {
            try {
                Project.Client.Client.INSTANCE.joinAsSpectator();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        add(spectateButton, BorderLayout.SOUTH);

    }

    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        if (Constants.LOBBY.equals(roomName) && isJoin) {
            setVisible(false);
            revalidate();
            repaint();
        }
    }

    @Override
    public void onReceivePhase(Phase phase) {
        System.out.println("Received phase: " + phase.name());
        if (!isVisible()) {
            setVisible(true);
            getParent().revalidate();
            getParent().repaint();
            System.out.println("GamePanel visible");
        }
        if (phase == Phase.READY) {
            cardLayout.show(playPanel.getParent(), READY_PANEL);
            buttonPanel.setVisible(false);
        } else if (phase == Phase.IN_PROGRESS) {
            cardLayout.show(playPanel.getParent(), PLAY_PANEL);
            buttonPanel.setVisible(true);
        }
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // Not used here, but needs to be defined due to interface
    }


    @Override
    public void onWordUpdate(String wordDisplay) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            wordDisplayLabel.setText(wordDisplay);
            resetLetterGrid(); // Reset letter buttons at the start of each round
            revalidate();
            repaint();
        });
    }

    @Override
    public void onStrikesUpdate(int strikes) {
        System.out.println("onStrikesUpdate called with strikes = " + strikes); // DEBUG
        javax.swing.SwingUtilities.invokeLater(() -> {
            strikesLabel.setText(String.format("Strikes: %d/6", strikes));
            hangmanPanel.setStrikes(strikes);
            revalidate();
            repaint();
        });
    }

    @Override
    public void onTookTurn(long clientId, boolean didTakeTurn) {
        boolean myTurn = (clientId == Project.Client.Client.INSTANCE.getMyClientId()) && didTakeTurn;
        isMyTurn = myTurn;
        javax.swing.SwingUtilities.invokeLater(() -> {
            guessInput.setEnabled(myTurn);
            letterInput.setEnabled(myTurn);
            for (JButton btn : letterButtons.values()) {
                if (hardMode) {
                    btn.setEnabled(myTurn);
                } else {
                    btn.setEnabled(myTurn);
                }
            }
            // Fix the "Your turn!" text
            String label = wordDisplayLabel.getText().replace(" (Your turn!)", "");
            if (myTurn) {
                wordDisplayLabel.setText(label + " (Your turn!)");
            } else {
                wordDisplayLabel.setText(label);
            }
        });
    }

    public void setHardMode(boolean hardMode) {
        this.hardMode = hardMode;
        // Optionally update the checkbox if needed
        // hardModeBox.setSelected(hardMode);
    }

    // Call this to disable a letter button after a guess, unless hardMode is enabled
    public void disableLetterButton(char letter) {
        if (hardMode) return; // In hard mode, do not disable any letter buttons
        JButton btn = letterButtons.get(Character.toUpperCase(letter));
        if (btn != null) {
            btn.setEnabled(false);
        }
    }

    // Reset all letter buttons to enabled
    public void resetLetterGrid() {
        for (JButton btn : letterButtons.values()) {
            btn.setEnabled(true);
        }
    }
}

// HangmanPanel for drawing the hangman
class HangmanPanel extends JPanel {
    private int strikes = 0;
    public void setStrikes(int strikes) {
        this.strikes = strikes;
        repaint();
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        // Draw gallows (classic style)
        g.drawLine(20, 180, 120, 180); // base
        g.drawLine(40, 180, 40, 20);   // pole
        g.drawLine(40, 20, 100, 20);   // top
        g.drawLine(100, 20, 100, 40);  // rope
        // Draw hangman parts based on strikes (stick figure style)
        if (strikes > 0) g.drawOval(85, 40, 30, 30); // head (centered on rope)
        if (strikes > 1) g.drawLine(100, 70, 100, 120); // body
        if (strikes > 2) g.drawLine(100, 80, 80, 100); // left arm
        if (strikes > 3) g.drawLine(100, 80, 120, 100); // right arm
        if (strikes > 4) g.drawLine(100, 120, 85, 150); // left leg
        if (strikes > 5) g.drawLine(100, 120, 115, 150); // right leg
    }
}