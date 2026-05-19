package edu.cmsc137.submarine.network;

import edu.cmsc137.submarine.core.GameState;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Game server that manages connections and synchronizes game state for all clients.
 * Runs on one PC and handles all connected players.
 */
public class GameServer {
    private final GameState gameState;
    private final int port;
    private final Map<Integer, ClientHandler> connectedClients;
    private final BlockingQueue<NetworkMessage> messageQueue;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private Thread gameLoopThread;
    private volatile boolean running;
    private int nextPlayerId = 1;
    private final Object clientLock = new Object();

    public GameServer(GameState gameState, int port) {
        this.gameState = gameState;
        this.port = port;
        this.connectedClients = new ConcurrentHashMap<>();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.running = false;
    }

    public void start() {
        if (running) {
            System.out.println("Server already running");
            return;
        }

        running = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Game server started on port " + port);

            // Thread to accept new client connections
            acceptThread = new Thread(this::acceptConnections);
            acceptThread.setName("GameServer-Accept");
            acceptThread.setDaemon(true);
            acceptThread.start();

            // Thread to process messages and update game state
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

        synchronized (clientLock) {
            for (ClientHandler handler : connectedClients.values()) {
                handler.disconnect();
            }
            connectedClients.clear();
        }

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
                // Process incoming messages from clients
                while (!messageQueue.isEmpty()) {
                    NetworkMessage msg = messageQueue.poll();
                    processMessage(msg);
                }

                // Periodically send game state to all clients
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSyncTime >= SYNC_INTERVAL) {
                    broadcastGameStateUpdate();
                    lastSyncTime = currentTime;
                }

                Thread.sleep(16); // ~60 FPS

            } catch (InterruptedException e) {
                if (running) {
                    System.err.println("Game loop interrupted: " + e.getMessage());
                }
            }
        }
    }

    private void processMessage(NetworkMessage message) {
        switch (message.getType()) {
            case PLAYER_MOVE:
                handlePlayerMove(message);
                break;
            case PLAYER_ACTION:
                handlePlayerAction(message);
                break;
            case ITEM_PICKUP:
                handleItemPickup(message);
                break;
            case ITEM_DROP:
                handleItemDrop(message);
                break;
            case DISCONNECT:
                handlePlayerDisconnect(message.getPlayerId());
                break;
            default:
                break;
        }
    }

    private void handlePlayerMove(NetworkMessage message) {
        // Data format: [dx, dy]
        if (message.getData() instanceof double[] movement) {
            // Update local game state (this would be integrated with multi-player state)
            System.out.println("Player " + message.getPlayerId() + " moved: " + Arrays.toString(movement));
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
        System.out.println("Player " + playerId + " disconnected");
        
        // Broadcast disconnect to remaining clients
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
        
        // Add all connected players to the snapshot
        synchronized (clientLock) {
            for (Map.Entry<Integer, ClientHandler> entry : connectedClients.entrySet()) {
                PlayerState playerState = new PlayerState(entry.getKey(), "Player " + entry.getKey());
                // TODO: Get actual player position from game state
                playerState.x = 100 + entry.getKey() * 50; // Temporary positioning
                playerState.y = 100 + entry.getKey() * 50;
                snapshot.playerStates.put(entry.getKey(), playerState);
            }
        }

        System.out.println("Broadcasting game state with " + snapshot.playerStates.size() + " players to " + connectedClients.size() + " clients");
        
        NetworkMessage stateMsg = new NetworkMessage(
            NetworkMessage.MessageType.GAME_STATE_UPDATE,
            0,
            snapshot
        );
        broadcastMessage(stateMsg);
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
                // Initialize streams (output first to avoid deadlock)
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.flush();
                inputStream = new ObjectInputStream(socket.getInputStream());
                connected = true;

                // Send player ID to client
                PlayerState playerState = new PlayerState(playerId, "Player " + playerId);
                NetworkMessage joinResponse = new NetworkMessage(
                    NetworkMessage.MessageType.JOIN_RESPONSE,
                    playerId,
                    playerState
                );
                sendMessage(joinResponse);

                // Notify all clients of new player
                server.broadcastMessage(joinResponse);

                // Listen for incoming messages
                while (connected && server.running) {
                    try {
                        Object obj = inputStream.readObject();
                        if (obj instanceof NetworkMessage) {
                            NetworkMessage message = (NetworkMessage) obj;
                            server.queueMessage(message);
                        }
                    } catch (EOFException e) {
                        // Client disconnected cleanly
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
