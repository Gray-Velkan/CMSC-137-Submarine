package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.core.Player;
import edu.cmsc137.submarine.core.TaskStation;
import edu.cmsc137.submarine.core.ItemType;
import edu.cmsc137.submarine.network.GameClient;
import edu.cmsc137.submarine.network.GameServer;
import edu.cmsc137.submarine.network.GameStateSnapshot;
import edu.cmsc137.submarine.network.NetworkMessage;
import edu.cmsc137.submarine.network.PlayerState;
import java.awt.*;
import java.util.Map;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Extended GamePanel that renders multiple players and synchronizes state with a server.
 * Skips the title/menu/loading screens since the LobbyPanel handles that flow.
 */
public class MultiplayerGamePanel extends GamePanel {
    private final GameServer server;
    private final GameClient client;
    private final Map<Integer, PlayerState> remotePlayerStates;
    private int localPlayerId = -1;

    private static final int REMOTE_PLAYER_WIDTH = 26;
    private static final int REMOTE_PLAYER_HEIGHT = 26;
    private static final Color[] PLAYER_COLORS = {
        new Color(72, 176, 255),   // blue (local)
        new Color(255, 100, 100),  // red
        new Color(100, 255, 100),  // green
        new Color(255, 200, 100),  // orange
        new Color(200, 100, 255),  // purple
        new Color(100, 255, 255),  // cyan
        new Color(255, 255, 100),  // yellow
        new Color(200, 200, 200)   // gray
    };

    private ScheduledExecutorService networkExecutor;

    public MultiplayerGamePanel(GameServer server, GameClient client) {
        super();
        this.server = server;
        this.client = client;
        this.remotePlayerStates = new ConcurrentHashMap<>();

        // Skip title/menu/loading — the lobby already handled that flow
        this.currentScreen = ScreenState.GAME;

        // Stop menu music that parent constructor auto-started, start game music
        if (menuMusic != null) {
            menuMusic.stop();
        }
        if (gameMusic != null && isMusicOn) {
            gameMusic.playLooping(0);
        }

        // Set local player ID from client if available
        if (client != null && client.getPlayerId() >= 0) {
            this.localPlayerId = client.getPlayerId();
        }

        if (client != null) {
            startNetworkProcessing();
        }
    }

    @Override
    protected void updateState(double deltaSeconds) {
        // Process all pending network messages before updating
        processAllNetworkMessages();

        // Send local player state to server
        if (client != null && client.isConnected() && client.getPlayerId() >= 0) {
            // Update local player ID if it was assigned after construction
            if (localPlayerId < 0) {
                localPlayerId = client.getPlayerId();
            }

            Player localPlayer = getGameState().getPlayer();
            PlayerState localState = new PlayerState(client.getPlayerId(), client.getPlayerName());
            localState.x = localPlayer.getX();
            localState.y = localPlayer.getY();
            localState.facingX = localPlayer.getFacingX();
            localState.facingY = localPlayer.getFacingY();
            localState.currentItem = getGameState().getHeldItemType();
            localState.pumping = getGameState().isPumping();
            localState.interactPressed = getInputHandler().isInteractPressed();
            localState.isActive = true;
            client.sendPlayerState(localState);
        }

        // Update local switch routing offline/fallback, server will override via snapshots
        boolean switchActive = false;
        for (TaskStation station : taskStations) {
            if (station.name.equals("Reactor Switch")) {
                if (getGameState().isNearTaskStation(station.centerX(), station.centerY(), 78.0)
                        && getInputHandler().isInteractPressed()) {
                    switchActive = true;
                }
                break;
            }
        }
        getGameState().setPowerRoutedToPump(switchActive);

        // Let the parent update the local game state (timer, buoyancy drain, etc.)
        // The server's authoritative state will be applied when we receive snapshots
        super.updateState(deltaSeconds);
    }

    @Override
    protected void tryUseNearbyTaskStation() {
        TaskStation station = findNearbyTaskStation();
        if (station == null) {
            return;
        }

        // Reactor Switch is a hold task, it doesn't give buoyancy directly on click.
        if (station.name.equals("Reactor Switch")) {
            return;
        }

        ItemType held = getGameState().getHeldItemType();
        if (station.requiredItem == held) {
            if (client != null && client.isConnected()) {
                // Send interaction request to the server
                client.sendPlayerAction("INTERACT:" + station.name);
            } else {
                super.tryUseNearbyTaskStation();
            }
        }
    }

    private void processAllNetworkMessages() {
        if (client == null || !client.isConnected()) {
            return;
        }

        // Drain all pending messages
        NetworkMessage msg;
        while ((msg = client.getIncomingMessage()) != null) {
            handleNetworkMessage(msg);
        }
    }

    private void startNetworkProcessing() {
        // The main game loop already calls processAllNetworkMessages() via updateState(),
        // but we also poll in the background to ensure messages aren't dropped during
        // pauses or screen transitions. This is a safety net.
        networkExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MultiplayerNetworkPoller");
            t.setDaemon(true);
            return t;
        });
        // No-op poller — actual processing happens in updateState on the game thread
        // This just keeps the executor alive for clean shutdown
    }

    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case JOIN_RESPONSE -> {
                if (message.getData() instanceof PlayerState state) {
                    // Update local player ID if this is our response
                    if (client != null && state.playerId == client.getPlayerId()) {
                        localPlayerId = state.playerId;
                    }
                    // Track all players (including self)
                    remotePlayerStates.put(state.playerId, state);
                }
            }
            case GAME_STATE_UPDATE -> {
                if (message.getData() instanceof GameStateSnapshot snapshot) {
                    applyServerSnapshot(snapshot);
                }
            }
            case PLAYER_DISCONNECTED -> {
                remotePlayerStates.remove(message.getPlayerId());
            }
            default -> {
                // Ignore other message types
            }
        }
    }

    /**
     * Apply the server's authoritative game state to the local GameState.
     * This keeps buoyancy, timer, and game-over status in sync across all clients.
     */
    private void applyServerSnapshot(GameStateSnapshot snapshot) {
        // Update shared submarine state from server
        getGameState().setBuoyancy(snapshot.submarineBuoyancy);
        getGameState().setTimeRemainingSeconds(snapshot.timeRemainingSeconds);
        getGameState().setSank(snapshot.sank);
        getGameState().setPowerRoutedToPump(snapshot.isPowerRoutedToPump);

        // Update all remote player positions
        if (snapshot.playerStates != null) {
            remotePlayerStates.clear();
            remotePlayerStates.putAll(snapshot.playerStates);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Let parent draw the full game (map, items, local player, HUD)
        super.paintComponent(g);

        // Only draw multiplayer elements when in the GAME screen
        if (currentScreen != ScreenState.GAME) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply the same scaling transform the parent used
        double scaleX = (double) getWidth() / PANEL_WIDTH;
        double scaleY = (double) getHeight() / PANEL_HEIGHT;
        g2.scale(scaleX, scaleY);

        // Draw remote players on top of the game scene
        drawRemotePlayers(g2);

        // Draw player count indicator
        drawPlayerCount(g2);

        // Reset the transform so it doesn't accumulate
        g2.setTransform(new java.awt.geom.AffineTransform());
    }

    private void drawRemotePlayers(Graphics2D g2) {
        int colorIndex = 1;
        for (Map.Entry<Integer, PlayerState> entry : remotePlayerStates.entrySet()) {
            PlayerState playerState = entry.getValue();

            // Skip the local player — they're already drawn by the parent
            if (playerState.playerId == localPlayerId) {
                continue;
            }

            if (!playerState.isActive) {
                continue;
            }

            Color playerColor = PLAYER_COLORS[colorIndex % PLAYER_COLORS.length];
            int px = (int) Math.round(playerState.x);
            int py = (int) Math.round(playerState.y);

            // Draw player body
            g2.setColor(playerColor);
            g2.fillRoundRect(px, py, REMOTE_PLAYER_WIDTH, REMOTE_PLAYER_HEIGHT, 8, 8);

            // Draw outline
            g2.setColor(new Color(14, 24, 34));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(px, py, REMOTE_PLAYER_WIDTH, REMOTE_PLAYER_HEIGHT, 8, 8);

            // Draw player name above their head
            g2.setColor(playerColor);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            String label = playerState.playerName != null ? playerState.playerName : "P" + playerState.playerId;
            FontMetrics fm = g2.getFontMetrics();
            int labelX = px + (REMOTE_PLAYER_WIDTH - fm.stringWidth(label)) / 2;
            g2.drawString(label, labelX, py - 4);

            // Draw held item indicator if they're holding something
            if (playerState.currentItem != null &&
                playerState.currentItem != edu.cmsc137.submarine.core.ItemType.NONE) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
                g2.setColor(new Color(255, 240, 200));
                String itemName = playerState.currentItem.name().toLowerCase().replace('_', ' ');
                g2.drawString("[" + itemName + "]", px - 8, py + REMOTE_PLAYER_HEIGHT + 12);
            }

            colorIndex++;
        }
    }

    private void drawPlayerCount(Graphics2D g2) {
        int totalPlayers = getVisibleRemotePlayerCount() + 1;
        g2.setColor(new Color(100, 200, 255, 200));
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.drawString("Players: " + totalPlayers, PANEL_WIDTH - 100, 30);
    }

    private int getVisibleRemotePlayerCount() {
        int count = 0;
        for (PlayerState state : remotePlayerStates.values()) {
            if (state.playerId != localPlayerId && state.isActive) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void stopGameLoop() {
        super.stopGameLoop();
        if (networkExecutor != null && !networkExecutor.isShutdown()) {
            networkExecutor.shutdownNow();
        }
        // Note: don't disconnect client/server here — MainFrame manages their lifecycle
    }
}
