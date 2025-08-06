package Project.Client.Views;


import Project.Client.Client;
import java.awt.GridLayout;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class ReadyPanel extends JPanel {
    private JCheckBox removeStrikeBox;
    private JCheckBox hardModeBox;
    private boolean isSessionCreator = false; // TODO: Set this based on actual session creator logic

    public ReadyPanel() {
        setLayout(new GridLayout(0, 1));
        removeStrikeBox = new JCheckBox("Remove strike on correct guess");
        hardModeBox = new JCheckBox("Hard Mode");
        removeStrikeBox.setVisible(isSessionCreator);
        hardModeBox.setVisible(isSessionCreator);
        this.add(removeStrikeBox);
        this.add(hardModeBox);

        JButton readyButton = new JButton();
        readyButton.setText("Ready");
        readyButton.addActionListener(event -> {
            try {

                // Send options to server if session creator
                if (isSessionCreator) {
                    boolean removeStrike = removeStrikeBox.isSelected();
                    boolean hardMode = hardModeBox.isSelected();
                    Client.INSTANCE.sendSessionOptions(removeStrike, hardMode);
                }

                Client.INSTANCE.sendReady();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        this.add(readyButton);
    }

    public void setSessionCreator(boolean isCreator) {
        this.isSessionCreator = isCreator;
        removeStrikeBox.setVisible(isCreator);
        hardModeBox.setVisible(isCreator);
    }

}