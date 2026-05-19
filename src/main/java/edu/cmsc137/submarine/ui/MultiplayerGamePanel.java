package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.network.GameClient;
import edu.cmsc137.submarine.network.GameServer;
import edu.cmsc137.submarine.network.GameStateSnapshot;
import edu.cmsc137.submarine.network.MultiplayerGameState;
import edu.cmsc137.submarine.network.NetworkMessage;
import edu.cmsc137.submarine.network.PlayerState;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Extended GamePanel that renders multiple players and synchronizes state with a server.
 */
public class MultiplayerGamePanel extends GamePanel {
    private final GameServer server;
    private final GameClient client;
    private final MultiplayerGameState multiplayerGameState;
    private static final int REMOTE_PLAYER_WIDTH = 26;
    private static final int REMOTE_PLAYER_HEIGHT = 26;
    private static final Color[] PLAYER_COLORS = {
        new Color(72, 176, 255),   // Blue (local player)
        new Color(255, 100, 100),  // Red
        new Color(100, 255, 100),  // Green
        new Color(255, 200, 100),  // Orange
        new Color(200, 100, 255),  // Purple
        new Color(100, 255, 255),  // Cyan
        new Color(255, 255, 100),  // Yellow
        new Color(200, 200, 200)   // Gray
    };

    public MultiplayerGamePanel(GameServer server, GameClient client) {
        super();
        this.server = server;
        this.client = client;
        
        // Initialize multiplayer state using dimensions from the parent GameState
        this.multiplayerGameState = new MultiplayerGameState(
            PANEL_WIDTH,
            PANEL_HEIGHT,
            180.0
        );
        
        // Set initial local player ID
        if (client != null) {
            multiplayerGameState.setLocalPlayerId(client.getPlayerId());
        } else if (server != null) {
            multiplayerGameState.setLocalPlayerId(0); // Host is usually ID 0
        }
        
        if (client != null) {
            startNetworkProcessing();
        }
    }

    /**
     * Overridden update loop to sync local position to the server.
     */
    @Override
    protected void updateState(double deltaSeconds) {
        // 1. Run the original GamePanel movement/collision logic
        super.updateState(deltaSeconds); 

        // 2. Send the updated position to the server
        if (client != null && client.isConnected()) {
            double px = getGameState().getPlayerX();
            double py = getGameState().getPlayerY();
            client.sendPlayerMove(px, py);
        }
    }

    private void startNetworkProcessing() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                if (client != null && client.isConnected()) {
                    NetworkMessage msg = client.getIncomingMessage();
                    if (msg != null) {
                        handleNetworkMessage(msg);
                    }
                } else {
                    executor.shutdown();
                }
            } catch (Exception e) {
                System.err.println("Network processing error: " + e.getMessage());
                executor.shutdown();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
    }

    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case JOIN_RESPONSE -> {
                if (message.getData() instanceof PlayerState state) {
                    // Update our internal ID if the server just confirmed who we are
                    if (client != null && state.playerId == client.getPlayerId()) {
                        multiplayerGameState.setLocalPlayerId(state.playerId);
                        System.out.println("Network identity confirmed: Player ID " + state.playerId);
                    }
                    multiplayerGameState.updateRemotePlayerState(state.playerId, state);
                }
            }
            case GAME_STATE_UPDATE -> {
                if (message.getData() instanceof GameStateSnapshot snapshot) {
                    // Sync all remote players' positions from the server's master state
                    multiplayerGameState.updateFromSnapshot(snapshot);
                    repaint(); 
                }
            }
            case PLAYER_DISCONNECTED -> {
                System.out.println("Player " + message.getPlayerId() + " left the game.");
                multiplayerGameState.removeRemotePlayer(message.getPlayerId());
                repaint();
            }
            default -> {}
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawRemotePlayers(g2);
        drawPlayerCount(g2);

        if (getGameState().isRoundOver()) {
            drawRoundEndOverlay(g2);
        }
    }

    private void drawRemotePlayers(Graphics2D g2) {
        int colorIndex = 1; 
        for (PlayerState playerState : multiplayerGameState.getAllRemotePlayerStates()) {
            // Don't draw a remote square for our own local character
            if (playerState.playerId == multiplayerGameState.getLocalPlayerId()) {
                continue; 
            }

            Color playerColor = PLAYER_COLORS[colorIndex % PLAYER_COLORS.length];
            int px = (int) Math.round(playerState.x);
            int py = (int) Math.round(playerState.y);

            g2.setColor(playerColor);
            g2.fillRoundRect(px, py, REMOTE_PLAYER_WIDTH, REMOTE_PLAYER_HEIGHT, 8, 8);

            g2.setColor(new Color(14, 24, 34));
            g2.drawRoundRect(px, py, REMOTE_PLAYER_WIDTH, REMOTE_PLAYER_HEIGHT, 8, 8);

            // Draw Label
            g2.setColor(playerColor);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            String label = playerState.playerName != null ? playerState.playerName : "P" + playerState.playerId;
            g2.drawString(label, px - 8, py - 4);

            colorIndex++;
        }
    }

    private void drawPlayerCount(Graphics2D g2) {
        int totalPlayers = multiplayerGameState.getRemotePlayerCount() + 1;
        g2.setColor(new Color(100, 200, 255));
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.drawString("Connected Players: " + totalPlayers, PANEL_WIDTH - 160, 30);
    }

    private void drawRoundEndOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        String message = getGameState().hasWon() ? "Victory!" : "Game Over";
        g2.drawString(message, PANEL_WIDTH / 2 - 50, PANEL_HEIGHT / 2 - 20);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        g2.drawString("Click to return to title", PANEL_WIDTH / 2 - 100, PANEL_HEIGHT / 2 + 20);
    }

    @Override
    protected void processInput(double deltaSeconds) {
        if (getGameState().isRoundOver() && getInputHandler().consumeClick()) {
            returnToTitle();
        }

        super.processInput(deltaSeconds);
    }

    private void returnToTitle() {
        System.out.println("Returning to title screen...");
        // Notify the main frame or game launcher to switch screens
    }

    @Override
    public void stopGameLoop() {
        super.stopGameLoop();
        if (client != null) {
            client.disconnect();
        }
        if (server != null) {
            server.stop();
        }
    }
}