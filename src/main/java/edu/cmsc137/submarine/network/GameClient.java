package edu.cmsc137.submarine.network;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Game client that connects to the server and syncs game state.
 * Runs on each player''s PC.
 */
public class GameClient {
    private final String serverHost;
    private final int serverPort;
    private final BlockingQueue<NetworkMessage> incomingMessages;
    private final BlockingQueue<NetworkMessage> outgoingMessages;
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Thread receiveThread;
    private Thread sendThread;
    private volatile boolean connected;
    private int playerId;
    private final String playerName;

    public GameClient(String serverHost, int serverPort, String playerName) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.playerName = playerName;
        this.incomingMessages = new LinkedBlockingQueue<>();
        this.outgoingMessages = new LinkedBlockingQueue<>();
        this.connected = false;
        this.playerId = -1;
    }

    public boolean connect() {
        return connectWithRetry(5, 500);
    }

    private boolean connectWithRetry(int maxAttempts, int delayMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                System.out.println("Connection attempt " + attempt + "/" + maxAttempts + " to " + serverHost + ":" + serverPort);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverHost, serverPort), NetworkConstants.CONNECTION_TIMEOUT_MS);
                socket.setSoTimeout(0); // no read timeout — server sends data periodically once game starts
                socket.setKeepAlive(true);

                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.flush();
                inputStream = new ObjectInputStream(socket.getInputStream());

                connected = true;
                System.out.println("Connected to server at " + serverHost + ":" + serverPort);

                receiveThread = new Thread(this::receiveLoop);
                receiveThread.setName("GameClient-Receive");
                receiveThread.setDaemon(true);
                receiveThread.start();

                sendThread = new Thread(this::sendLoop);
                sendThread.setName("GameClient-Send");
                sendThread.setDaemon(true);
                sendThread.start();

                PlayerState state = new PlayerState(0, playerName);
                NetworkMessage joinMsg = new NetworkMessage(
                    NetworkMessage.MessageType.JOIN_GAME,
                    0,
                    state
                );
                sendMessage(joinMsg);

                return true;

            } catch (ConnectException e) {
                System.err.println("Connection failed (attempt " + attempt + "/" + maxAttempts + "): " + e.getMessage());
                connected = false;

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            } catch (IOException e) {
                System.err.println("I/O error (attempt " + attempt + "/" + maxAttempts + "): " + e.getMessage());
                connected = false;

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        System.err.println("Failed to connect after " + maxAttempts + " attempts");
        return false;
    }

    public void disconnect() {
        connected = false;

        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }

        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        if (sendThread != null) {
            sendThread.interrupt();
        }

        System.out.println("Disconnected from server");
    }

    private void receiveLoop() {
        while (connected) {
            try {
                Object obj = inputStream.readObject();
                if (obj instanceof NetworkMessage message) {
                    if (message.getType() == NetworkMessage.MessageType.JOIN_RESPONSE) {
                        if (message.getData() instanceof PlayerState state) {
                            this.playerId = state.playerId;
                            System.out.println("Assigned player ID: " + playerId);
                        }
                    }

                    incomingMessages.put(message);
                }
            } catch (EOFException e) {
                System.out.println("Server closed connection");
                break;
            } catch (SocketException e) {
                if (connected) {
                    System.err.println("Socket error: " + e.getMessage());
                }
                break;
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    System.err.println("Receive error: " + e.getMessage());
                }
                break;
            } catch (InterruptedException e) {
                System.err.println("Receive thread interrupted");
                break;
            }
        }
        connected = false;
    }

    private void sendLoop() {
        while (connected) {
            try {
                NetworkMessage message = outgoingMessages.poll(100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    synchronized (this) {
                        outputStream.reset();
                        outputStream.writeObject(message);
                        outputStream.flush();
                    }
                }
            } catch (SocketException e) {
                if (connected) {
                    System.err.println("Socket error during send: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Send error: " + e.getMessage());
                }
                break;
            } catch (InterruptedException e) {
                // Timeout, continue polling
            }
        }
    }

    public void sendMessage(NetworkMessage message) {
        if (message.getPlayerId() == 0) {
            message = new NetworkMessage(message.getType(), playerId, message.getData());
        }
        try {
            outgoingMessages.put(message);
        } catch (InterruptedException e) {
            System.err.println("Error queuing outgoing message: " + e.getMessage());
        }
    }

    public void sendPlayerMove(double dx, double dy) {
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.PLAYER_MOVE,
            playerId,
            new double[]{dx, dy}
        );
        sendMessage(msg);
    }

    public void sendPlayerState(PlayerState state) {
        if (state == null) {
            return;
        }

        PlayerState payload = new PlayerState(playerId, state.playerName != null ? state.playerName : playerName);
        payload.x = state.x;
        payload.y = state.y;
        payload.facingX = state.facingX;
        payload.facingY = state.facingY;
        payload.currentItem = state.currentItem;
        payload.pumping = state.pumping;
        payload.isActive = state.isActive;

        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.PLAYER_MOVE,
            playerId,
            payload
        );
        sendMessage(msg);
    }

    public NetworkMessage getIncomingMessage() {
        return incomingMessages.poll();
    }

    public NetworkMessage waitForIncomingMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return incomingMessages.poll(timeout, unit);
    }

    public boolean isConnected() {
        return connected;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void sendPlayerAction(String action) {
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.PLAYER_ACTION,
            playerId,
            action
        );
        sendMessage(msg);
    }

    public void sendItemPickup() {
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.ITEM_PICKUP,
            playerId
        );
        sendMessage(msg);
    }

    public void sendItemDrop() {
        NetworkMessage msg = new NetworkMessage(
            NetworkMessage.MessageType.ITEM_DROP,
            playerId
        );
        sendMessage(msg);
    }
}
