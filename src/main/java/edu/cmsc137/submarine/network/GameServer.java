package edu.cmsc137.submarine.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class GameServer implements Runnable {
    private static final int PORT = 8080;

    private final int requiredPlayers;
    private final Object lock = new Object();
    private final List<ClientSession> clients = new ArrayList<>();

    private volatile boolean running = true;
    private volatile boolean gameStarted = false;
    private volatile String hostPlayerId;
    private volatile ServerSocket serverSocket;
    private int nextPlayerNumber = 1;

    public GameServer(int requiredPlayers) {
        if (requiredPlayers != 2 && requiredPlayers != 4) {
            throw new IllegalArgumentException("requiredPlayers must be 2 or 4");
        }
        this.requiredPlayers = requiredPlayers;
    }

    @Override
    public void run() {
        try (ServerSocket listeningSocket = new ServerSocket(PORT)) {
            serverSocket = listeningSocket;
            System.out.println("GameServer: listening on port " + PORT + ", waiting for players...");

            while (running && !gameStarted) {
                Socket socket;
                try {
                    socket = listeningSocket.accept();
                } catch (SocketException ex) {
                    if (running) {
                        System.err.println("GameServer: accept interrupted: " + ex.getMessage());
                    }
                    break;
                }

                if (!running || gameStarted) {
                    closeQuietly(socket);
                    break;
                }

                if (isLobbyFull()) {
                    closeQuietly(socket);
                    continue;
                }

                initializeClient(socket);
            }

            if (gameStarted) {
                update();
            }
        } catch (IOException ex) {
            if (running) {
                System.err.println("GameServer failed: " + ex.getMessage());
            }
        } finally {
            shutdown();
        }
    }

    private void initializeClient(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new PacketObjectInputStream(socket.getInputStream());

            ClientSession session = new ClientSession(socket, in, out, "P" + nextPlayerNumber, "Player " + nextPlayerNumber);
            nextPlayerNumber++;

            synchronized (lock) {
                clients.add(session);
                if (hostPlayerId == null) {
                    hostPlayerId = session.playerId;
                }
            }

            sendPacket(session, new WelcomePacket(session.playerId));
            broadcastLobbyState();

            Thread clientThread = new Thread(() -> handleClient(session), "server-client-" + session.playerId);
            clientThread.setDaemon(true);
            clientThread.start();

            System.out.println("GameServer: accepted " + session.playerId + " from " + socket.getRemoteSocketAddress());
        } catch (IOException ex) {
            closeQuietly(socket);
            System.err.println("GameServer: failed to initialize client: " + ex.getMessage());
        }
    }

    private boolean isLobbyFull() {
        synchronized (lock) {
            return clients.size() >= requiredPlayers;
        }
    }

    private void handleClient(ClientSession session) {
        try {
            while (running && !session.socket.isClosed()) {
                Object packet = session.in.readObject();
                handlePacket(session, packet);
            }
        } catch (EOFException | SocketException ignored) {
            // Connection dropped or stream ended.
        } catch (IOException | ClassNotFoundException ignored) {
            // Invalid stream state or unsupported payload.
        } finally {
            removeClient(session);
        }
    }

    private void handlePacket(ClientSession session, Object packet) {
        if (packet instanceof ClientHelloPacket hello) {
            String name = sanitizeName(hello.getPlayerName());
            synchronized (lock) {
                session.playerName = name;
            }
            broadcastLobbyState();
            return;
        }

        if (packet instanceof StartGameRequestPacket request) {
            boolean canStart;
            synchronized (lock) {
                canStart = !gameStarted
                    && session.playerId.equals(hostPlayerId)
                    && session.playerId.equals(request.getRequesterId())
                    && clients.size() >= 2;
            }
            if (canStart) {
                startGame();
            }
        }
        if (packet instanceof PlayerStatePacket state) {
            // update last-known state for this session
            session.lastX = state.getX();
            session.lastY = state.getY();
            session.lastFacingX = state.getFacingX();
            session.lastFacingY = state.getFacingY();
            return;
        }
    }

    private String sanitizeName(String name) {
        if (name == null) {
            return "Player";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "Player";
        }
        return trimmed.length() > 20 ? trimmed.substring(0, 20) : trimmed;
    }

    private void removeClient(ClientSession session) {
        boolean changed = false;
        synchronized (lock) {
            if (clients.remove(session)) {
                changed = true;
                if (session.playerId.equals(hostPlayerId)) {
                    hostPlayerId = clients.isEmpty() ? null : clients.get(0).playerId;
                }
            }
        }

        closeQuietly(session.socket);

        if (changed && !gameStarted) {
            broadcastLobbyState();
        }
    }

    private void startGame() {
        synchronized (lock) {
            if (gameStarted) {
                return;
            }
            gameStarted = true;
        }

        System.out.println("GameServer: host started the game.");
        // build initial snapshots for players using last-known positions if available,
        // otherwise assign simple spawn positions spread out in the map center area.
        List<ClientSession> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(clients);
        }

        List<PlayerSnapshot> initialPlayers = new ArrayList<>(snapshot.size());
        double centerX = 640.0; // approximate center based on GamePanel.PANEL_WIDTH
        double centerY = 384.0;
        double spread = 80.0;
        for (int i = 0; i < snapshot.size(); i++) {
            ClientSession cs = snapshot.get(i);
            double x = cs.lastX != 0.0 ? cs.lastX : centerX + (i - snapshot.size() / 2.0) * spread;
            double y = cs.lastY != 0.0 ? cs.lastY : centerY;
            PlayerSnapshot ps = new PlayerSnapshot(cs.playerId, cs.playerName, x, y, cs.lastFacingX, cs.lastFacingY, cs.playerId.equals(hostPlayerId));
            initialPlayers.add(ps);
        }

        // send GameStart with initial positions
        GameStartPacket startPacket = new GameStartPacket(requiredPlayers, initialPlayers);
        for (ClientSession client : snapshot) {
            sendPacket(client, startPacket);
        }

        // start broadcaster thread to periodically send GameStateSnapshot
        Thread broadcaster = new Thread(() -> {
            try {
                while (running && gameStarted) {
                    List<ClientSession> snap;
                    synchronized (lock) {
                        snap = new ArrayList<>(clients);
                    }
                    List<PlayerSnapshot> players = new ArrayList<>(snap.size());
                    for (ClientSession cs : snap) {
                        PlayerSnapshot ps = new PlayerSnapshot(cs.playerId, cs.playerName, cs.lastX, cs.lastY, cs.lastFacingX, cs.lastFacingY, cs.playerId.equals(hostPlayerId));
                        players.add(ps);
                    }
                    GameStateSnapshotPacket gsp = new GameStateSnapshotPacket(players);
                    for (ClientSession cs : snap) {
                        sendPacket(cs, gsp);
                    }
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                // nothing
            }
        }, "game-state-broadcaster");
        broadcaster.setDaemon(true);
        broadcaster.start();

        closeServerSocket();
    }

    private void broadcastLobbyState() {
        List<ClientSession> snapshot;
        String currentHostId;
        synchronized (lock) {
            snapshot = new ArrayList<>(clients);
            currentHostId = hostPlayerId;
        }

        List<String> players = new ArrayList<>(snapshot.size());
        for (ClientSession client : snapshot) {
            String label = client.playerName;
            if (client.playerId.equals(currentHostId)) {
                label += " (Host)";
            }
            players.add(label);
        }

        LobbyStatePacket state = new LobbyStatePacket(players, requiredPlayers, currentHostId);
        for (ClientSession client : snapshot) {
            sendPacket(client, state);
        }
    }

    private void broadcastGameStart() {
        List<ClientSession> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(clients);
        }
        List<PlayerSnapshot> initialPlayers = new ArrayList<>(snapshot.size());
        double centerX = 640.0;
        double centerY = 384.0;
        double spread = 80.0;
        for (int i = 0; i < snapshot.size(); i++) {
            ClientSession cs = snapshot.get(i);
            double x = cs.lastX != 0.0 ? cs.lastX : centerX + (i - snapshot.size() / 2.0) * spread;
            double y = cs.lastY != 0.0 ? cs.lastY : centerY;
            PlayerSnapshot ps = new PlayerSnapshot(cs.playerId, cs.playerName, x, y, cs.lastFacingX, cs.lastFacingY, cs.playerId.equals(hostPlayerId));
            initialPlayers.add(ps);
        }

        GameStartPacket startPacket = new GameStartPacket(requiredPlayers, initialPlayers);
        for (ClientSession client : snapshot) {
            sendPacket(client, startPacket);
        }
    }

    private void sendPacket(ClientSession session, Object packet) {
        try {
            synchronized (session.out) {
                session.out.writeObject(packet);
                session.out.flush();
            }
        } catch (IOException ex) {
            System.err.println("GameServer: send failed for " + session.playerId + ": " + ex.getMessage());
        }
    }

    private void update() {
        System.out.println("GameServer: entering main update loop (placeholder)");
        synchronized (this) {
            while (running) {
                try {
                    wait(1000L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void shutdown() {
        running = false;
        synchronized (this) {
            notifyAll();
        }
        closeServerSocket();

        List<ClientSession> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(clients);
            clients.clear();
        }

        for (ClientSession session : snapshot) {
            closeQuietly(session.socket);
        }
    }

    private void closeServerSocket() {
        ServerSocket socket = serverSocket;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private static final class ClientSession {
        private final Socket socket;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;
        private final String playerId;
        private volatile String playerName;
        // last known position and facing received from client
        private volatile double lastX;
        private volatile double lastY;
        private volatile int lastFacingX;
        private volatile int lastFacingY;

        private ClientSession(Socket socket, ObjectInputStream in, ObjectOutputStream out, String playerId, String playerName) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.playerId = playerId;
            this.playerName = playerName;
            this.lastX = 0.0;
            this.lastY = 0.0;
            this.lastFacingX = 1;
            this.lastFacingY = 0;
        }
    }
}
