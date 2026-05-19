package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.network.GameClient;
import edu.cmsc137.submarine.network.GameServer;
import edu.cmsc137.submarine.network.NetworkConstants;
import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 * Lobby panel for connecting to multiplayer game or hosting a server.
 */
public class LobbyPanel extends JPanel {
    private final JTextField serverHostField;
    private final JTextField playerNameField;
    private final JButton hostGameButton;
    private final JButton joinGameButton;
    private final JButton backButton;
    private final JLabel statusLabel;

    private GameServer server;
    private GameClient client;
    private ActionListener onGameStart;
    private ActionListener onBackClick;
    private boolean isHostMode;  // Track if we're hosting

    public LobbyPanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(7, 14, 22));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Submarine Survival - Multiplayer Lobby");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.CYAN);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        // Player name
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel nameLabel = new JLabel("Player Name:");
        nameLabel.setForeground(Color.WHITE);
        add(nameLabel, gbc);

        gbc.gridx = 1;
        playerNameField = new JTextField("Player", 15);
        add(playerNameField, gbc);

        // Server host
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel hostLabel = new JLabel("Server Host:");
        hostLabel.setForeground(Color.WHITE);
        add(hostLabel, gbc);

        gbc.gridx = 1;
        serverHostField = new JTextField(NetworkConstants.SERVER_HOST, 15);
        add(serverHostField, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        hostGameButton = new JButton("Host Game");
        hostGameButton.setBackground(new Color(34, 139, 34));
        hostGameButton.setForeground(Color.WHITE);
        hostGameButton.addActionListener(e -> hostGame());
        add(hostGameButton, gbc);

        gbc.gridx = 1;
        joinGameButton = new JButton("Join Game");
        joinGameButton.setBackground(new Color(30, 144, 255));
        joinGameButton.setForeground(Color.WHITE);
        joinGameButton.addActionListener(e -> joinGame());
        add(joinGameButton, gbc);

        // Status label
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.GREEN);
        add(statusLabel, gbc);

        // Back button
        gbc.gridy = 5;
        backButton = new JButton("Back");
        backButton.setBackground(new Color(139, 69, 19));
        backButton.setForeground(Color.WHITE);
        backButton.addActionListener(e -> {
            if (onBackClick != null) {
                onBackClick.actionPerformed(null);
            }
        });
        add(backButton, gbc);

        setPreferredSize(new Dimension(400, 300));
    }

    private void hostGame() {
        setStatus("Starting server...", Color.YELLOW);
        hostGameButton.setEnabled(false);
        joinGameButton.setEnabled(false);

        new Thread(() -> {
            try {
                isHostMode = true;  // Mark as host mode
                setStatus("Server ready on port " + NetworkConstants.SERVER_PORT, Color.GREEN);
                try {
                    Thread.sleep(500); // Show message briefly
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (onGameStart != null) {
                    onGameStart.actionPerformed(null);
                }
            } catch (Exception e) {
                setStatus("Error: " + e.getMessage(), Color.RED);
                hostGameButton.setEnabled(true);
                joinGameButton.setEnabled(true);
            }
        }).start();
    }

    private void joinGame() {
        String playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            setStatus("Enter a player name", Color.RED);
            return;
        }

        String host = serverHostField.getText().trim();
        if (host.isEmpty()) {
            setStatus("Enter server host", Color.RED);
            return;
        }

        setStatus("Connecting to server... (this may take a few seconds)", Color.YELLOW);
        hostGameButton.setEnabled(false);
        joinGameButton.setEnabled(false);

        new Thread(() -> {
            try {
                client = new GameClient(host, NetworkConstants.SERVER_PORT, playerName);
                if (client.connect()) {
                    setStatus("Connected! Player ID: " + client.getPlayerId(), Color.GREEN);
                    try {
                        Thread.sleep(1000); // Show success message briefly
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (onGameStart != null) {
                        onGameStart.actionPerformed(null);
                    }
                } else {
                    setStatus("Failed to connect to server - check host and try again", Color.RED);
                    hostGameButton.setEnabled(true);
                    joinGameButton.setEnabled(true);
                }
            } catch (Exception e) {
                setStatus("Error: " + e.getMessage(), Color.RED);
                hostGameButton.setEnabled(true);
                joinGameButton.setEnabled(true);
            }
        }).start();
    }

    private void setStatus(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        });
    }

    public void setOnGameStart(ActionListener listener) {
        this.onGameStart = listener;
    }

    public void setOnBackClick(ActionListener listener) {
        this.onBackClick = listener;
    }

    public GameServer getServer() {
        return server;
    }

    public void setServer(GameServer server) {
        this.server = server;
    }

    public GameClient getClient() {
        return client;
    }

    public void setClient(GameClient client) {
        this.client = client;
    }

    public boolean isHostMode() {
        return isHostMode;
    }

    public void resetGame() {
        isHostMode = false;
        server = null;
        client = null;
        setStatus("Ready", Color.GREEN);
        hostGameButton.setEnabled(true);
        joinGameButton.setEnabled(true);
    }
}
