package edu.cmsc137.submarine.network;

import edu.cmsc137.submarine.core.GameState;
import edu.cmsc137.submarine.core.ItemType;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Game server that manages connections and synchronizes game state for all clients.
 * Runs on one PC and handles all connected players.
 */
public class GameServer {
    private final GameState gameState;
    private final int port;
    private final Map<Integer, ClientHandler> connectedClients;
    private final Map<Integer, PlayerState> playerStates;
    private final BlockingQueue<NetworkMessage> messageQueue;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private Thread gameLoopThread;
    private volatile boolean running;
    private volatile boolean gameStarted;
    private int nextPlayerId = 1;
    private final Object clientLock = new Object();

    public GameServer(GameState gameState, int port) {
        this.gameState = gameState;
        this.port = port;
        this.connectedClients = new ConcurrentHashMap<>();
        this.playerStates = new ConcurrentHashMap<>();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.running = false;
        this.gameStarted = false;
    }

    public void start() {
        if (running) {
            System.out.println("Server already running");
            return;
        }

        running = true;
        gameStarted = false;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Game server started on port " + port);

            acceptThread = new Thread(this::acceptConnections);
            acceptThread.setName("GameServer-Accept");
            acceptThread.setDaemon(true);
            acceptThread.start();

            gameLoopThread = new Thread(this::gameLoop);
            gameLoopThread.setName("GameServer-GameLoop");
            gameLoopThread.setDaemon(true);
            gameLoopThread.start();

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            running = false;
        }
    }

    public void stop() {
        running = false;
        gameStarted = false;

        synchronized (clientLock) {
            for (ClientHandler handler : connectedClients.values()) {
                handler.disconnect();
            }
            connectedClients.clear();
        }
        playerStates.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        if (gameLoopThread != null) {
            gameLoopThread.interrupt();
        }

        System.out.println("Game server stopped");
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connection from " + clientSocket.getInetAddress());

                if (connectedClients.size() >= NetworkConstants.MAX_PLAYERS) {
                    System.out.println("Server full, rejecting connection");
                    clientSocket.close();
                    continue;
                }

                int playerId = nextPlayerId++;
                ClientHandler handler = new ClientHandler(this, clientSocket, playerId);
                synchronized (clientLock) {
                    connectedClients.put(playerId, handler);
                }
                new Thread(handler).start();

            } catch (SocketException e) {
                if (running) {
                    System.err.println("Socket error during accept: " + e.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    private void gameLoop() {
        long lastSyncTime = 0;
        final long SYNC_INTERVAL = NetworkConstants.STATE_SYNC_INTERVAL_MS;

        while (running) {
            try {
                while (!messageQueue.isEmpty()) {
                    NetworkMessage msg = messageQueue.poll();
                    if (msg != null) {
                        processMessage(msg);
                    }
                }

                // Only update game state (timer, buoyancy drain) when game has started
                if (gameStarted) {
                    gameState.update(1.0 / 60.0);
                }

                long currentTime = System.currentTimeMillis();
                if (gameStarted && currentTime - lastSyncTime >= SYNC_INTERVAL) {
                    broadcastGameStateUpdate();
                    lastSyncTime = currentTime;
                }

                Thread.sleep(16);

            } catch (InterruptedException e) {
                if (running) {
                    System.err.println("Game loop interrupted: " + e.getMessage());
                }
            }
        }
    }

    private void processMessage(NetworkMessage message) {
        switch (message.getType()) {
            case PLAYER_MOVE -> handlePlayerMove(message);
            case PLAYER_ACTION -> handlePlayerAction(message);
            case ITEM_PICKUP -> handleItemPickup(message);
            case ITEM_DROP -> handleItemDrop(message);
            case DISCONNECT -> handlePlayerDisconnect(message.getPlayerId());
            default -> {
            }
        }
    }

    private void handlePlayerMove(NetworkMessage message) {
        PlayerState playerState = playerStates.get(message.getPlayerId());
        if (playerState == null) {
            return;
        }

        if (message.getData() instanceof PlayerState update) {
            playerState.x = update.x;
            playerState.y = update.y;
            playerState.facingX = update.facingX;
            playerState.facingY = update.facingY;
            playerState.currentItem = update.currentItem;
            playerState.pumping = update.pumping;
            playerState.isActive = update.isActive;
            if (update.playerName != null && !update.playerName.isBlank()) {
                playerState.playerName = update.playerName;
            }
        } else if (message.getData() instanceof double[] movement && movement.length >= 2) {
            playerState.x = movement[0];
            playerState.y = movement[1];
        }
    }

    private void handlePlayerAction(NetworkMessage message) {
        System.out.println("Player " + message.getPlayerId() + " action: " + message.getData());
    }

    private void handleItemPickup(NetworkMessage message) {
        System.out.println("Player " + message.getPlayerId() + " picked up item");
    }

    private void handleItemDrop(NetworkMessage message) {
        System.out.println("Player " + message.getPlayerId() + " dropped item");
    }

    private void handlePlayerDisconnect(int playerId) {
        synchronized (clientLock) {
            connectedClients.remove(playerId);
        }
        playerStates.remove(playerId);
        System.out.println("Player " + playerId + " disconnected");

        NetworkMessage disconnectMsg = new NetworkMessage(
            NetworkMessage.MessageType.PLAYER_DISCONNECTED,
            playerId
        );
        broadcastMessage(disconnectMsg);
    }

    private void broadcastGameStateUpdate() {
        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.submarineBuoyancy = gameState.getBuoyancy();
        snapshot.timeRemainingSeconds = gameState.getTimeRemainingSeconds();
        snapshot.sank = gameState.hasLostBySinking();
        snapshot.gameOver = gameState.isRoundOver();

        synchronized (clientLock) {
            for (Map.Entry<Integer, PlayerState> entry : playerStates.entrySet()) {
                snapshot.playerStates.put(entry.getKey(), copyPlayerState(entry.getValue()));
            }
        }


        NetworkMessage stateMsg = new NetworkMessage(
            NetworkMessage.MessageType.GAME_STATE_UPDATE,
            0,
            snapshot
        );
        broadcastMessage(stateMsg);
    }

    public void beginGame() {
        if (!running || gameStarted) {
            return;
        }

        gameStarted = true;
        broadcastMessage(new NetworkMessage(NetworkMessage.MessageType.START_GAME, 0));
        broadcastGameStateUpdate();
    }

    public void queueMessage(NetworkMessage message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            System.err.println("Error queuing message: " + e.getMessage());
        }
    }

    public void broadcastMessage(NetworkMessage message) {
        synchronized (clientLock) {
            for (ClientHandler handler : connectedClients.values()) {
                handler.sendMessage(message);
            }
        }
    }

    public void sendMessageToClient(int playerId, NetworkMessage message) {
        ClientHandler handler = connectedClients.get(playerId);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    public int getConnectedPlayerCount() {
        return connectedClients.size();
    }

    public boolean isRunning() {
        return running;
    }

    public java.util.List<String> getPlayerNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (PlayerState state : playerStates.values()) {
            names.add(state.playerName != null ? state.playerName : "Player " + state.playerId);
        }
        return names;
    }

    // Valid spawn tiles from the map grid (tile coordinates on floor tiles)
    private static final int[][] SPAWN_TILES = {
        {12, 13}, {14, 13}, {10, 13}, {16, 13}, {8, 13},
        {5, 4}, {6, 4}, {21, 4}, {31, 7}
    };

    private PlayerState createSpawnPlayerState(int playerId, String playerName) {
        PlayerState state = new PlayerState(playerId, playerName);
        int spawnIndex = (playerId - 1) % SPAWN_TILES.length;
        state.x = SPAWN_TILES[spawnIndex][0] * 32 + 4;
        state.y = SPAWN_TILES[spawnIndex][1] * 32 + 3;
        state.facingX = 1;
        state.facingY = 0;
        state.currentItem = ItemType.NONE;
        state.pumping = false;
        state.isActive = true;
        return state;
    }

    private PlayerState copyPlayerState(PlayerState source) {
        PlayerState copy = new PlayerState(source.playerId, source.playerName);
        copy.x = source.x;
        copy.y = source.y;
        copy.facingX = source.facingX;
        copy.facingY = source.facingY;
        copy.currentItem = source.currentItem;
        copy.pumping = source.pumping;
        copy.isActive = source.isActive;
        return copy;
    }

    /**
     * Inner class to handle individual client connections
     */
    private class ClientHandler implements Runnable {
        private final GameServer server;
        private final Socket socket;
        private final int playerId;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private volatile boolean connected;

        public ClientHandler(GameServer server, Socket socket, int playerId) {
            this.server = server;
            this.socket = socket;
            this.playerId = playerId;
            this.connected = false;
        }

        @Override
        public void run() {
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.flush();
                inputStream = new ObjectInputStream(socket.getInputStream());
                connected = true;

                Object firstObject = inputStream.readObject();
                if (!(firstObject instanceof NetworkMessage joinRequest) || joinRequest.getType() != NetworkMessage.MessageType.JOIN_GAME) {
                    throw new IOException("Expected join request from client");
                }

                String playerName = "Player " + playerId;
                if (joinRequest.getData() instanceof PlayerState joinState && joinState.playerName != null && !joinState.playerName.isBlank()) {
                    playerName = joinState.playerName.trim();
                }

                PlayerState playerState = createSpawnPlayerState(playerId, playerName);
                playerStates.put(playerId, playerState);

                NetworkMessage joinResponse = new NetworkMessage(
                    NetworkMessage.MessageType.JOIN_RESPONSE,
                    playerId,
                    copyPlayerState(playerState)
                );

                // Send this new player their own state
                sendMessage(joinResponse);
                // Tell all other clients about the new player
                server.broadcastMessage(joinResponse);

                // Send the new player info about ALL existing players so their lobby is complete
                for (Map.Entry<Integer, PlayerState> entry : playerStates.entrySet()) {
                    if (entry.getKey() != playerId) {
                        NetworkMessage existingPlayer = new NetworkMessage(
                            NetworkMessage.MessageType.JOIN_RESPONSE,
                            entry.getKey(),
                            copyPlayerState(entry.getValue())
                        );
                        sendMessage(existingPlayer);
                    }
                }

                if (gameStarted) {
                    sendMessage(new NetworkMessage(NetworkMessage.MessageType.START_GAME, 0));
                }

                while (connected && server.running) {
                    try {
                        Object obj = inputStream.readObject();
                        if (obj instanceof NetworkMessage message) {
                            server.queueMessage(message);
                        }
                    } catch (EOFException e) {
                        break;
                    } catch (SocketException e) {
                        if (connected) {
                            System.err.println("Socket error for player " + playerId + ": " + e.getMessage());
                        }
                        break;
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error in client handler for player " + playerId + ": " + e.getMessage());
            } finally {
                disconnect();
                server.handlePlayerDisconnect(playerId);
            }
        }

        public void sendMessage(NetworkMessage message) {
            try {
                if (connected && outputStream != null) {
                    synchronized (this) {
                        outputStream.reset();
                        outputStream.writeObject(message);
                        outputStream.flush();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error sending message to player " + playerId + ": " + e.getMessage());
                disconnect();
            }
        }

        public void disconnect() {
            connected = false;
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client handler: " + e.getMessage());
            }
        }
    }
}

