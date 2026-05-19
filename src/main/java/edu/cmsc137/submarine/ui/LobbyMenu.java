package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.network.ClientHelloPacket;
import edu.cmsc137.submarine.network.GameClient;
import edu.cmsc137.submarine.network.GameServer;
import edu.cmsc137.submarine.network.GameStartPacket;
import edu.cmsc137.submarine.network.LobbyStatePacket;
import edu.cmsc137.submarine.network.StartGameRequestPacket;
import edu.cmsc137.submarine.network.WelcomePacket;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class LobbyMenu extends JFrame {
    private static final String CARD_HOME = "home";
    private static final String CARD_LOBBY = "lobby";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final JButton hostButton = new JButton("Host Game");
    private final JButton joinButton = new JButton("Join Game");
    private final JButton startGameButton = new JButton("Start Game");
    private final JButton leaveButton = new JButton("Leave Lobby");

    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel lobbyTitleLabel = new JLabel("Submarine Survival", SwingConstants.CENTER);
    private final JLabel roleLabel = new JLabel("Role: Guest");
    private final JLabel playerCountLabel = new JLabel("Players: 0/0");
    private final JLabel hostIpLabel = new JLabel("Host IP: ");
    private final JLabel playersHeaderLabel = new JLabel("Players in Lobby");

    private final DefaultListModel<String> playerListModel = new DefaultListModel<>();
    private final JList<String> playerList = new JList<>(playerListModel);

    private GameServer server;
    private GameClient client;
    private boolean isHost;
    private int requiredPlayers;
    private String localPlayerId;
    private String localPlayerName;

    public LobbyMenu() {
        super("Submarine Survival");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(560, 420));

        initializeLookAndFeel();
        buildHomeScreen();
        buildLobbyScreen();
        add(cards);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });

        setLocationRelativeTo(null);
    }

    private void initializeLookAndFeel() {
        UIManager.put("Button.arc", 14);
        UIManager.put("Component.arc", 14);
    }

    private void buildHomeScreen() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        root.setBackground(new Color(16, 33, 53));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Submarine Survival", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 42));
        title.setForeground(new Color(232, 247, 255));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Host or join a co-op lobby", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        subtitle.setForeground(new Color(174, 214, 235));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        top.add(Box.createVerticalGlue());
        top.add(title);
        top.add(Box.createVerticalStrut(8));
        top.add(subtitle);
        top.add(Box.createVerticalGlue());

        JPanel actions = new JPanel(new GridLayout(1, 2, 14, 0));
        actions.setOpaque(false);

        stylePrimaryButton(hostButton, new Color(27, 152, 224));
        stylePrimaryButton(joinButton, new Color(37, 205, 130));
        hostButton.addActionListener(event -> onHostClicked());
        joinButton.addActionListener(event -> onJoinClicked());
        actions.add(hostButton);
        actions.add(joinButton);

        JPanel statusWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusWrap.setOpaque(false);
        statusLabel.setForeground(new Color(204, 228, 243));
        statusWrap.add(statusLabel);

        root.add(top, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        root.add(statusWrap, BorderLayout.NORTH);

        cards.add(root, CARD_HOME);
    }

    private void buildLobbyScreen() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        root.setBackground(new Color(239, 246, 250));

        lobbyTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        lobbyTitleLabel.setForeground(new Color(16, 42, 68));

        JLabel lobbySubtitle = new JLabel("Lobby", SwingConstants.CENTER);
        lobbySubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        lobbySubtitle.setForeground(new Color(70, 99, 125));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(lobbyTitleLabel);
        titlePanel.add(lobbySubtitle);

        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 6));
        infoPanel.setOpaque(false);
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        roleLabel.setForeground(new Color(20, 78, 117));
        playerCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        playerCountLabel.setForeground(new Color(52, 84, 110));
        infoPanel.add(roleLabel);
        infoPanel.add(playerCountLabel);
        hostIpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hostIpLabel.setForeground(new Color(88, 115, 140));
        infoPanel.add(hostIpLabel);

        playerList.setBorder(BorderFactory.createLineBorder(new Color(190, 211, 227), 1));
        playerList.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        playerList.setFixedCellHeight(28);

        JScrollPane scrollPane = new JScrollPane(playerList);
        scrollPane.setPreferredSize(new Dimension(340, 220));

        playersHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        playersHeaderLabel.setForeground(new Color(20, 78, 117));

        JPanel playerSection = new JPanel(new BorderLayout(0, 8));
        playerSection.setOpaque(false);
        playerSection.add(playersHeaderLabel, BorderLayout.NORTH);
        playerSection.add(scrollPane, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(infoPanel, BorderLayout.NORTH);
        center.add(playerSection, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(1, 2, 12, 0));
        controls.setOpaque(false);

        stylePrimaryButton(startGameButton, new Color(8, 148, 96));
        stylePrimaryButton(leaveButton, new Color(108, 122, 138));
        startGameButton.addActionListener(event -> onStartGameClicked());
        leaveButton.addActionListener(event -> onLeaveClicked());
        startGameButton.setEnabled(false);

        controls.add(startGameButton);
        controls.add(leaveButton);

        root.add(titlePanel, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(controls, BorderLayout.SOUTH);

        cards.add(root, CARD_LOBBY);
    }

    private void stylePrimaryButton(JButton button, Color color) {
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFont(new Font("Segoe UI", Font.BOLD, 17));
    }

    private void onHostClicked() {
        JComboBox<Integer> combo = new JComboBox<>(new Integer[]{2, 4});
        combo.setSelectedItem(4);

        int result = JOptionPane.showConfirmDialog(this, combo, "Lobby Size", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        requiredPlayers = (Integer) combo.getSelectedItem();
        localPlayerName = askPlayerName("Enter your host name:");
        if (localPlayerName == null) {
            return;
        }

        setHomeButtonsEnabled(false);
        setStatus("Starting local server...");
        new Thread(() -> startServer(requiredPlayers), "lobby-start-server-thread").start();
        String ip = getLocalIpAddress();
        startClientAsync(ip == null ? "127.0.0.1" : ip, 8080, localPlayerName);
        SwingUtilities.invokeLater(() -> hostIpLabel.setText("Host IP: discovering..."));
        final String resolved = ip;
        new Thread(() -> {
            String finalIp = resolved;
            if (finalIp == null) {
                finalIp = getLocalIpAddress();
            }
            final String display = finalIp == null ? "127.0.0.1" : finalIp;
            SwingUtilities.invokeLater(() -> hostIpLabel.setText("Host IP: " + display + "  — share to let friends join"));
        }, "host-ip-resolver").start();
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress ia = addrs.nextElement();
                    String ip = ia.getHostAddress();
                    if (ip == null) continue;
                    // prefer IPv4 addresses
                    if (ip.contains(".")) {
                        return ip;
                    }
                }
            }
        } catch (SocketException ex) {
            // ignore and fallback
        }

        // Final fallback: try localhost
        try {
            InetAddress local = InetAddress.getLocalHost();
            String ip = local.getHostAddress();
            if (ip != null && ip.contains(".")) return ip;
        } catch (Exception ignored) {
        }

        return null;
    }

    private void onJoinClicked() {
        String ip = JOptionPane.showInputDialog(this, "Enter host IP address:", "Join Game", JOptionPane.PLAIN_MESSAGE);
        if (ip == null || ip.isBlank()) {
            return;
        }

        localPlayerName = askPlayerName("Enter your player name:");
        if (localPlayerName == null) {
            return;
        }

        requiredPlayers = 0;
        setHomeButtonsEnabled(false);
        setStatus("Connecting to " + ip.trim() + "...");
        startClientAsync(ip.trim(), 8080, localPlayerName);
    }

    private String askPlayerName(String message) {
        String name = JOptionPane.showInputDialog(this, message, "Player Name", JOptionPane.PLAIN_MESSAGE);
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? "Player" : trimmed;
    }

    private void startServer(int targetPlayers) {
        server = new GameServer(targetPlayers);
        Thread t = new Thread(server, "game-server-thread");
        t.setDaemon(true);
        t.start();
    }

    private void startClientAsync(String host, int port, String playerName) {
        client = new GameClient(packet -> SwingUtilities.invokeLater(() -> handleIncomingPacket(packet)));

        new Thread(() -> {
            try {
                client.connect(host, port);
                client.send(new ClientHelloPacket(playerName));
                SwingUtilities.invokeLater(() -> {
                    setStatus("Connected to " + host + ":" + port);
                    cardLayout.show(cards, CARD_LOBBY);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    resetToHomeState();
                    JOptionPane.showMessageDialog(this, "Failed to connect: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "lobby-client-connect-thread").start();
    }

    private void handleIncomingPacket(Object packet) {
        if (packet instanceof WelcomePacket welcome) {
            localPlayerId = welcome.getPlayerId();
            return;
        }

        if (packet instanceof LobbyStatePacket state) {
            applyLobbyState(state);
            return;
        }

        if (packet instanceof GameStartPacket start) {
            openGamePanel(start.getPlayers());
        }
    }

    private void applyLobbyState(LobbyStatePacket state) {
        List<String> players = state.getPlayers();
        requiredPlayers = state.getRequiredPlayers();

        playerListModel.clear();
        for (String player : players) {
            playerListModel.addElement(player);
        }

        playerList.revalidate();
        playerList.repaint();
        cards.revalidate();
        cards.repaint();

        isHost = localPlayerId != null && localPlayerId.equals(state.getHostPlayerId());
        roleLabel.setText(isHost ? "Role: Host" : "Role: Player");
        playerCountLabel.setText("Players: " + players.size() + "/" + requiredPlayers);

        boolean canHostStart = isHost && players.size() >= 2;
        startGameButton.setEnabled(canHostStart);
        startGameButton.setText(isHost ? "Start Game" : "Waiting for Host");

        if (isHost && players.size() < 2) {
            setStatus("Need at least 2 players to start.");
        } else if (isHost) {
            setStatus("You can start the match now.");
        } else {
            setStatus("Waiting for host to start the game.");
        }
    }

    private void onStartGameClicked() {
        if (!isHost || client == null || localPlayerId == null) {
            return;
        }
        try {
            client.send(new StartGameRequestPacket(localPlayerId));
            setStatus("Starting game...");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to start game: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onLeaveClicked() {
        cleanupNetworkOnly();
        resetToHomeState();
    }

    private void resetToHomeState() {
        isHost = false;
        localPlayerId = null;
        localPlayerName = null;
        requiredPlayers = 0;
        playerListModel.clear();
        roleLabel.setText("Role: Guest");
        playerCountLabel.setText("Players: 0/0");
        startGameButton.setEnabled(false);
        startGameButton.setText("Start Game");
        setHomeButtonsEnabled(true);
        setStatus("Ready");
        cardLayout.show(cards, CARD_HOME);
    }

    private void setHomeButtonsEnabled(boolean enabled) {
        hostButton.setEnabled(enabled);
        joinButton.setEnabled(enabled);
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void cleanupNetworkOnly() {
        if (client != null) {
            client.close();
            client = null;
        }
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    private void cleanup() {
        cleanupNetworkOnly();
    }

    private void openGamePanel(java.util.List<edu.cmsc137.submarine.network.PlayerSnapshot> initialPlayers) {
        try {
            dispose();

            JFrame gameFrame = new JFrame("Submarine Survival");
            GamePanel panel = new GamePanel(client, localPlayerId);

            // set client listener to forward game snapshots to the panel
            if (client != null) {
                client.setListener(packet -> {
                    if (packet instanceof edu.cmsc137.submarine.network.GameStateSnapshotPacket snap) {
                        panel.updateRemotePlayers(snap.getPlayers());
                        return;
                    }
                    // other packets may be handled here if needed
                });
            }

            if (initialPlayers != null) {
                panel.updateRemotePlayers(initialPlayers);
            }

            gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gameFrame.getContentPane().add(panel);
            gameFrame.pack();
            gameFrame.setLocationRelativeTo(null);
            gameFrame.setVisible(true);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Failed to open game: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showLobby() {
        SwingUtilities.invokeLater(() -> {
            LobbyMenu lobby = new LobbyMenu();
            lobby.setVisible(true);
        });
    }
}
