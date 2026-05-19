package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.core.GameState;
import edu.cmsc137.submarine.network.GameClient;
import edu.cmsc137.submarine.network.GameServer;
import edu.cmsc137.submarine.network.NetworkConstants;
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

        // Create and add lobby panel
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

        // Show lobby first
        cardLayout.show(cardPanel, LOBBY_PANEL);
    }

    private void startGame() {
        // Check if this is host or client mode from the lobby
        if (lobbyPanel.isHostMode()) {
            // Host mode - create and start server
            startServerGame();
        } else {
            // Client mode - get client from lobby and connect
            client = lobbyPanel.getClient();
            if (client != null && client.isConnected()) {
                startClientGame();
            }
        }
    }

    private void startServerGame() {
        // Create game state for server
        GameState gameState = new GameState(
            1280,  // Width - adjust based on your map
            768,   // Height - adjust based on your map
            180.0  // Initial time in seconds
        );

        // Create and start server
        server = new GameServer(gameState, NetworkConstants.SERVER_PORT);
        server.start();
        
        System.out.println("Server created and started");
        
        // Give server time to bind and start accepting connections
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Create multiplayer game panel for host (NOT single-player GamePanel)
        gamePanel = new MultiplayerGamePanel(server, null);  // server=not null, client=null for host
        setupGamePanel(gamePanel);

        cardPanel.add(gamePanel, GAME_PANEL);
        cardLayout.show(cardPanel, GAME_PANEL);
        pack();
    }

    private void startClientGame() {
        // Create multiplayer game panel that connects to server
        gamePanel = new MultiplayerGamePanel(null, client);
        setupGamePanel(gamePanel);

        cardPanel.add(gamePanel, GAME_PANEL);
        cardLayout.show(cardPanel, GAME_PANEL);
        pack();
    }

    private void setupGamePanel(GamePanel panel) {
        setTitle("CMSC 137 Submarine - In Game");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.stopGameLoop();
                cleanup();
            }
        });
    }

    private void backToLobby() {
        if (gamePanel != null) {
            gamePanel.stopGameLoop();
            cardPanel.remove(gamePanel);
        }

        cleanup();

        // Reset lobby for next game
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
