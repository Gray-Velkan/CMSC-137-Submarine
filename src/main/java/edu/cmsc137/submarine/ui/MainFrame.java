package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.network.GameClient;
import edu.cmsc137.submarine.network.GameServer;
import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Main application frame that handles switching between Lobby and Game views.
 */
public class MainFrame extends JFrame {
    private static final String LOBBY_PANEL = "lobby";
    private static final String GAME_PANEL = "game";

    private final JPanel cardPanel;
    private final CardLayout cardLayout;
    private final LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private GameServer server;
    private GameClient client;

    public MainFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("CMSC 137 Submarine - Networked Multiplayer");
        setResizable(false);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        lobbyPanel = new LobbyPanel();
        lobbyPanel.setOnGameStart(e -> startGame());
        lobbyPanel.setOnBackClick(e -> backToLobby());
        cardPanel.add(lobbyPanel, LOBBY_PANEL);

        setContentPane(cardPanel);
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });

        cardLayout.show(cardPanel, LOBBY_PANEL);
    }

    private void startGame() {
        server = lobbyPanel.getServer();
        client = lobbyPanel.getClient();

        if (client == null || !client.isConnected()) {
            return;
        }

        gamePanel = new MultiplayerGamePanel(server, client);
        gamePanel.setOnExit(e -> backToLobby());
        setupGamePanel(gamePanel);

        cardPanel.add(gamePanel, GAME_PANEL);
        cardLayout.show(cardPanel, GAME_PANEL);
        pack();
        gamePanel.requestFocusInWindow();
    }

    private void setupGamePanel(GamePanel panel) {
        setTitle("CMSC 137 Submarine - In Game");
    }

    private void backToLobby() {
        if (gamePanel != null) {
            gamePanel.stopGameLoop();
            cardPanel.remove(gamePanel);
            gamePanel = null;
        }

        cleanup();

        lobbyPanel.resetGame();
        cardLayout.show(cardPanel, LOBBY_PANEL);
        setTitle("CMSC 137 Submarine - Networked Multiplayer");
        pack();
        setLocationRelativeTo(null);
    }

    private void cleanup() {
        if (gamePanel != null) {
            gamePanel.stopGameLoop();
        }

        if (client != null && client.isConnected()) {
            client.disconnect();
        }

        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}
