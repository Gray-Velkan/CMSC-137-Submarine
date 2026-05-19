package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.core.GameState;
import edu.cmsc137.submarine.network.GameClient;
import edu.cmsc137.submarine.network.GameServer;
import edu.cmsc137.submarine.network.NetworkConstants;
import edu.cmsc137.submarine.network.NetworkMessage;
import edu.cmsc137.submarine.network.PlayerState;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class LobbyPanel extends JPanel {
    private static final int W = 1024, H = 614;
    private static final Color BG_DARK = new Color(7, 14, 22);
    private static final Color CYAN_GLOW = new Color(0, 220, 255);
    private static final Color CYAN_DIM = new Color(0, 150, 200);
    private static final Color GREEN_BTN = new Color(34, 160, 60);
    private static final Color BLUE_BTN = new Color(30, 120, 220);
    private static final Color RED_BTN = new Color(160, 40, 40);
    private static final Color PANEL_BG = new Color(12, 22, 35, 200);
    private static final Color FIELD_BG = new Color(20, 35, 55);
    private static final Color FIELD_BORDER = new Color(0, 140, 200, 150);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 36);
    private static final Font SUBTITLE_FONT = new Font("Arial", Font.BOLD, 20);
    private static final Font BODY_FONT = new Font("Arial", Font.PLAIN, 16);
    private static final Font BTN_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font SMALL_FONT = new Font("Arial", Font.PLAIN, 13);
    private static final Font PLAYER_FONT = new Font("Arial", Font.BOLD, 15);

    private enum Step { CHOICE, NAME, ADDRESS, LOBBY }

    private Step currentStep = Step.CHOICE;
    private boolean hostMode = false;
    private boolean gameStartTriggered = false;
    private String statusText = "Choose how you want to play";
    private Color statusColor = CYAN_DIM;

    private String playerName = "Player";
    private String serverHost = NetworkConstants.SERVER_HOST;

    private GameServer server;
    private GameClient client;
    private ActionListener onGameStart;
    private ActionListener onBackClick;
    private final Timer lobbyTimer;
    private final List<String> lobbyPlayerNames = new ArrayList<>();

    // Input field bounds (for click detection)
    private final Rectangle nameFieldRect = new Rectangle(0, 0, 0, 0);
    private final Rectangle hostFieldRect = new Rectangle(0, 0, 0, 0);
    private boolean nameFieldFocused = false;
    private boolean hostFieldFocused = false;
    private String nameFieldText = "Player";
    private String hostFieldText = NetworkConstants.SERVER_HOST;

    // Button bounds
    private final Rectangle btn1Rect = new Rectangle();
    private final Rectangle btn2Rect = new Rectangle();
    private final Rectangle btn3Rect = new Rectangle();
    private Rectangle hoveredBtn = null;

    private BufferedImage lobbyBgImage;

    public LobbyPanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(BG_DARK);
        setFocusable(true);

        try {
            lobbyBgImage = ImageIO.read(new File("assets/lobby.png"));
        } catch (IOException e) {
            System.err.println("Could not load lobby.png");
        }

        lobbyTimer = new Timer(200, e -> refreshLobbyState());
        lobbyTimer.setRepeats(true);

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                double sx = (double) getWidth() / W;
                double sy = (double) getHeight() / H;
                int mx = (int)(e.getX() / sx), my = (int)(e.getY() / sy);
                handleClick(mx, my);
            }
        });

        addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                double sx = (double) getWidth() / W;
                double sy = (double) getHeight() / H;
                int mx = (int)(e.getX() / sx), my = (int)(e.getY() / sy);
                Rectangle prev = hoveredBtn;
                hoveredBtn = null;
                if (btn1Rect.contains(mx, my)) hoveredBtn = btn1Rect;
                else if (btn2Rect.contains(mx, my)) hoveredBtn = btn2Rect;
                else if (btn3Rect.contains(mx, my)) hoveredBtn = btn3Rect;
                if (hoveredBtn != prev) repaint();
            }
        });

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if (nameFieldFocused) {
                    if (c == '\b') {
                        if (!nameFieldText.isEmpty()) nameFieldText = nameFieldText.substring(0, nameFieldText.length()-1);
                    } else if (c == '\n' || c == '\r') {
                        handleBtn1Click();
                    } else if (nameFieldText.length() < 20 && !Character.isISOControl(c)) {
                        nameFieldText += c;
                    }
                    repaint();
                } else if (hostFieldFocused) {
                    if (c == '\b') {
                        if (!hostFieldText.isEmpty()) hostFieldText = hostFieldText.substring(0, hostFieldText.length()-1);
                    } else if (c == '\n' || c == '\r') {
                        handleBtn1Click();
                    } else if (hostFieldText.length() < 40 && !Character.isISOControl(c)) {
                        hostFieldText += c;
                    }
                    repaint();
                }
            }
        });
    }

    private void handleClick(int mx, int my) {
        // Check text fields
        if (currentStep == Step.NAME) {
            nameFieldFocused = nameFieldRect.contains(mx, my);
            hostFieldFocused = false;
        } else if (currentStep == Step.ADDRESS) {
            hostFieldFocused = hostFieldRect.contains(mx, my);
            nameFieldFocused = false;
        }

        if (btn1Rect.contains(mx, my)) handleBtn1Click();
        else if (btn2Rect.contains(mx, my)) handleBtn2Click();
        else if (btn3Rect.contains(mx, my)) handleBtn3Click();
        requestFocusInWindow();
        repaint();
    }

    private void handleBtn1Click() {
        switch (currentStep) {
            case CHOICE -> { hostMode = true; currentStep = Step.NAME; nameFieldFocused = true; }
            case NAME -> {
                playerName = nameFieldText.trim();
                if (playerName.isEmpty()) { setStatus("Enter a name!", Color.RED); return; }
                if (hostMode) startHosting(playerName);
                else { currentStep = Step.ADDRESS; hostFieldFocused = true; }
            }
            case ADDRESS -> joinGame();
            case LOBBY -> { if (hostMode) startHostedGame(); }
        }
        repaint();
    }

    private void handleBtn2Click() {
        switch (currentStep) {
            case CHOICE -> { hostMode = false; currentStep = Step.NAME; nameFieldFocused = true; }
            case NAME -> { currentStep = Step.CHOICE; nameFieldFocused = false; }
            case ADDRESS -> { currentStep = Step.NAME; hostFieldFocused = false; nameFieldFocused = true; }
            case LOBBY -> { cleanupSession(); currentStep = Step.CHOICE; }
        }
        repaint();
    }

    private void handleBtn3Click() {
        if (currentStep == Step.CHOICE && onBackClick != null) {
            onBackClick.actionPerformed(null);
        }
    }

    // ===================== PAINTING =====================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        double sx = (double) getWidth() / W, sy = (double) getHeight() / H;
        g2.scale(sx, sy);

        // Background
        if (lobbyBgImage != null) {
            g2.drawImage(lobbyBgImage, 0, 0, W, H, null);
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, W, H);
        } else {
            g2.setColor(BG_DARK);
            g2.fillRect(0, 0, W, H);
        }

        switch (currentStep) {
            case CHOICE -> paintChoice(g2);
            case NAME -> paintName(g2);
            case ADDRESS -> paintAddress(g2);
            case LOBBY -> paintLobby(g2);
        }

        // Status bar at bottom
        g2.setFont(SMALL_FONT);
        g2.setColor(statusColor);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(statusText, (W - fm.stringWidth(statusText)) / 2, H - 20);

        g2.dispose();
    }

    private void paintChoice(Graphics2D g2) {
        // Center panel
        int pw = 460, ph = 320;
        int px = (W - pw) / 2, py = (H - ph) / 2 - 20;
        drawPanel(g2, px, py, pw, ph);

        // Title
        g2.setFont(TITLE_FONT);
        g2.setColor(CYAN_GLOW);
        drawCentered(g2, "SUBMARINE SURVIVAL", W / 2, py + 55);

        g2.setFont(BODY_FONT);
        g2.setColor(new Color(180, 200, 220));
        drawCentered(g2, "Networked Multiplayer", W / 2, py + 85);

        // Host Game button
        int bw = 200, bh = 52;
        int bx1 = W / 2 - bw - 15, by1 = py + 130;
        setRect(btn1Rect, bx1, by1, bw, bh);
        drawButton(g2, btn1Rect, "HOST GAME", GREEN_BTN, hoveredBtn == btn1Rect);

        // Join Game button
        int bx2 = W / 2 + 15;
        setRect(btn2Rect, bx2, by1, bw, bh);
        drawButton(g2, btn2Rect, "JOIN GAME", BLUE_BTN, hoveredBtn == btn2Rect);

        // Back button
        int bw3 = 180, bh3 = 40;
        setRect(btn3Rect, (W - bw3) / 2, py + ph - 70, bw3, bh3);
        drawButton(g2, btn3Rect, "BACK TO TITLE", RED_BTN, hoveredBtn == btn3Rect);
    }

    private void paintName(Graphics2D g2) {
        int pw = 440, ph = 260;
        int px = (W - pw) / 2, py = (H - ph) / 2 - 20;
        drawPanel(g2, px, py, pw, ph);

        g2.setFont(SUBTITLE_FONT);
        g2.setColor(CYAN_GLOW);
        drawCentered(g2, "ENTER YOUR NAME", W / 2, py + 50);

        // Name field
        int fw = 320, fh = 40;
        int fx = (W - fw) / 2, fy = py + 80;
        setRect(nameFieldRect, fx, fy, fw, fh);
        drawTextField(g2, nameFieldRect, nameFieldText, nameFieldFocused);

        // Continue button
        int bw = 160, bh = 44;
        setRect(btn1Rect, W / 2 + 10, py + ph - 75, bw, bh);
        drawButton(g2, btn1Rect, "CONTINUE", GREEN_BTN, hoveredBtn == btn1Rect);

        // Back button
        setRect(btn2Rect, W / 2 - bw - 10, py + ph - 75, bw, bh);
        drawButton(g2, btn2Rect, "BACK", RED_BTN, hoveredBtn == btn2Rect);

        btn3Rect.setBounds(0, 0, 0, 0);
    }

    private void paintAddress(Graphics2D g2) {
        int pw = 440, ph = 260;
        int px = (W - pw) / 2, py = (H - ph) / 2 - 20;
        drawPanel(g2, px, py, pw, ph);

        g2.setFont(SUBTITLE_FONT);
        g2.setColor(CYAN_GLOW);
        drawCentered(g2, "JOIN A HOST", W / 2, py + 50);

        g2.setFont(BODY_FONT);
        g2.setColor(new Color(180, 200, 220));
        drawCentered(g2, "Enter the host's IP address", W / 2, py + 78);

        int fw = 320, fh = 40;
        int fx = (W - fw) / 2, fy = py + 95;
        setRect(hostFieldRect, fx, fy, fw, fh);
        drawTextField(g2, hostFieldRect, hostFieldText, hostFieldFocused);

        int bw = 160, bh = 44;
        setRect(btn1Rect, W / 2 + 10, py + ph - 75, bw, bh);
        drawButton(g2, btn1Rect, "CONNECT", BLUE_BTN, hoveredBtn == btn1Rect);

        setRect(btn2Rect, W / 2 - bw - 10, py + ph - 75, bw, bh);
        drawButton(g2, btn2Rect, "BACK", RED_BTN, hoveredBtn == btn2Rect);

        btn3Rect.setBounds(0, 0, 0, 0);
    }

    private void paintLobby(Graphics2D g2) {
        int pw = 520, ph = 400;
        int px = (W - pw) / 2, py = (H - ph) / 2 - 15;
        drawPanel(g2, px, py, pw, ph);

        // Header
        g2.setFont(SUBTITLE_FONT);
        g2.setColor(CYAN_GLOW);
        drawCentered(g2, hostMode ? "HOSTING LOBBY" : "CONNECTED TO HOST", W / 2, py + 40);

        g2.setFont(SMALL_FONT);
        g2.setColor(new Color(140, 170, 200));
        String info = hostMode ? "Port " + NetworkConstants.SERVER_PORT + " · Waiting for players..." : "Waiting for host to start the game...";
        drawCentered(g2, info, W / 2, py + 62);

        // Player list area
        int listX = px + 30, listY = py + 80, listW = pw - 60, listH = ph - 175;
        g2.setColor(new Color(8, 16, 28, 180));
        g2.fill(new RoundRectangle2D.Double(listX, listY, listW, listH, 12, 12));
        g2.setColor(new Color(0, 100, 150, 100));
        g2.setStroke(new BasicStroke(1));
        g2.draw(new RoundRectangle2D.Double(listX, listY, listW, listH, 12, 12));

        g2.setFont(SMALL_FONT);
        g2.setColor(CYAN_DIM);
        g2.drawString("CONNECTED PLAYERS", listX + 15, listY + 20);

        // Draw player cards
        int cardY = listY + 35;
        int cardH = 36;
        Color[] playerColors = {
            new Color(72, 176, 255), new Color(255, 100, 100),
            new Color(100, 255, 100), new Color(255, 200, 100),
            new Color(200, 100, 255), new Color(100, 255, 255),
            new Color(255, 255, 100), new Color(200, 200, 200)
        };

        if (lobbyPlayerNames.isEmpty()) {
            g2.setFont(BODY_FONT);
            g2.setColor(new Color(100, 120, 140));
            drawCentered(g2, "No players yet...", W / 2, cardY + 30);
        } else {
            for (int i = 0; i < lobbyPlayerNames.size() && i < 8; i++) {
                int cy = cardY + i * (cardH + 6);
                if (cy + cardH > listY + listH - 5) break;
                Color pc = playerColors[i % playerColors.length];

                // Card background
                g2.setColor(new Color(pc.getRed() / 8, pc.getGreen() / 8, pc.getBlue() / 8, 150));
                g2.fill(new RoundRectangle2D.Double(listX + 12, cy, listW - 24, cardH, 8, 8));

                // Color indicator
                g2.setColor(pc);
                g2.fillRoundRect(listX + 20, cy + 8, 20, 20, 6, 6);

                // Player name
                g2.setFont(PLAYER_FONT);
                g2.setColor(Color.WHITE);
                g2.drawString(lobbyPlayerNames.get(i), listX + 50, cy + 24);

                // Player number
                g2.setFont(SMALL_FONT);
                g2.setColor(new Color(120, 140, 160));
                g2.drawString("P" + (i + 1), listX + listW - 50, cy + 23);
            }
        }

        // Player count
        g2.setFont(BODY_FONT);
        g2.setColor(CYAN_GLOW);
        drawCentered(g2, "Players: " + lobbyPlayerNames.size(), W / 2, py + ph - 80);

        // Buttons
        int bw = 160, bh = 44;
        if (hostMode) {
            setRect(btn1Rect, W / 2 + 10, py + ph - 60, bw, bh);
            boolean canStart = !lobbyPlayerNames.isEmpty();
            drawButton(g2, btn1Rect, "START GAME", canStart ? GREEN_BTN : new Color(60, 70, 60), hoveredBtn == btn1Rect && canStart);
        } else {
            btn1Rect.setBounds(0, 0, 0, 0);
        }

        setRect(btn2Rect, hostMode ? W / 2 - bw - 10 : (W - bw) / 2, py + ph - 60, bw, bh);
        drawButton(g2, btn2Rect, "CANCEL", RED_BTN, hoveredBtn == btn2Rect);
        btn3Rect.setBounds(0, 0, 0, 0);
    }

    // ===================== DRAW HELPERS =====================

    private void drawPanel(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(PANEL_BG);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 20, 20));
        g2.setColor(new Color(0, 180, 255, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 20, 20));
    }

    private void drawButton(Graphics2D g2, Rectangle r, String text, Color base, boolean hovered) {
        Color bg = hovered ? base.brighter() : base;
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 14, 14));
        if (hovered) {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height / 2, 14, 14));
        }
        g2.setColor(new Color(255, 255, 255, 50));
        g2.setStroke(new BasicStroke(1));
        g2.draw(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 14, 14));
        g2.setFont(BTN_FONT);
        g2.setColor(Color.WHITE);
        drawCentered(g2, text, r.x + r.width / 2, r.y + r.height / 2 + 6);
    }

    private void drawTextField(Graphics2D g2, Rectangle r, String text, boolean focused) {
        g2.setColor(FIELD_BG);
        g2.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 10, 10));
        g2.setColor(focused ? CYAN_GLOW : FIELD_BORDER);
        g2.setStroke(new BasicStroke(focused ? 2f : 1f));
        g2.draw(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 10, 10));
        g2.setFont(BODY_FONT);
        g2.setColor(Color.WHITE);
        String display = text + (focused && System.currentTimeMillis() % 1000 < 500 ? "|" : "");
        g2.drawString(display, r.x + 12, r.y + r.height / 2 + 6);
    }

    private void drawCentered(Graphics2D g2, String text, int cx, int cy) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy);
    }

    private void setRect(Rectangle r, int x, int y, int w, int h) {
        r.setBounds(x, y, w, h);
    }

    // ===================== NETWORKING =====================

    private void startHosting(String name) {
        setStatus("Starting server...", Color.YELLOW);
        new Thread(() -> {
            try {
                server = new GameServer(new GameState(1280, 768, 180.0), NetworkConstants.SERVER_PORT);
                server.start();
                client = new GameClient(NetworkConstants.SERVER_HOST, NetworkConstants.SERVER_PORT, name);
                if (!client.connect()) throw new IllegalStateException("Host client failed to connect");

                // Wait for playerId
                long t0 = System.currentTimeMillis();
                while (client.getPlayerId() < 0 && System.currentTimeMillis() - t0 < 5000) {
                    client.getIncomingMessage(); // drain
                    Thread.sleep(50);
                }

                SwingUtilities.invokeLater(() -> {
                    setStatus("Hosting on port " + NetworkConstants.SERVER_PORT, Color.GREEN);
                    currentStep = Step.LOBBY;
                    lobbyTimer.start();
                    repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("Error: " + e.getMessage(), Color.RED);
                    currentStep = Step.NAME;
                    repaint();
                });
            }
        }, "HostStarter").start();
    }

    private void joinGame() {
        String host = hostFieldText.trim();
        if (host.isEmpty()) { setStatus("Enter a host IP!", Color.RED); return; }
        playerName = nameFieldText.trim();
        setStatus("Connecting to " + host + "...", Color.YELLOW);
        repaint();

        new Thread(() -> {
            try {
                client = new GameClient(host, NetworkConstants.SERVER_PORT, playerName);
                if (!client.connect()) throw new IllegalStateException("Connection failed");

                // Wait for playerId — the receiveLoop sets it from JOIN_RESPONSE
                long t0 = System.currentTimeMillis();
                while (client.getPlayerId() < 0 && System.currentTimeMillis() - t0 < 5000) {
                    Thread.sleep(50);
                }

                // Drain any pending messages and collect player names
                final List<String> initialPlayers = new ArrayList<>();
                initialPlayers.add(playerName); // Add self
                NetworkMessage msg;
                while ((msg = client.getIncomingMessage()) != null) {
                    if (msg.getType() == NetworkMessage.MessageType.JOIN_RESPONSE && msg.getData() instanceof PlayerState ps) {
                        if (ps.playerName != null && !ps.playerName.equals(playerName) && !initialPlayers.contains(ps.playerName)) {
                            initialPlayers.add(ps.playerName);
                        }
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    setStatus("Connected! Player ID: " + client.getPlayerId(), Color.GREEN);
                    lobbyPlayerNames.clear();
                    lobbyPlayerNames.addAll(initialPlayers);
                    currentStep = Step.LOBBY;
                    lobbyTimer.start();
                    repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("Error: " + e.getMessage(), Color.RED);
                    repaint();
                });
            }
        }, "JoinStarter").start();
    }

    private void startHostedGame() {
        if (server == null) return;
        server.beginGame();
        fireGameStart();
    }

    private void fireGameStart() {
        if (gameStartTriggered) return;
        gameStartTriggered = true;
        lobbyTimer.stop();
        if (onGameStart != null) onGameStart.actionPerformed(null);
    }

    private void refreshLobbyState() {
        // Update player list
        if (server != null) {
            List<String> names = server.getPlayerNames();
            lobbyPlayerNames.clear();
            lobbyPlayerNames.addAll(names);
        }

        // Process client messages (check for START_GAME from server)
        if (client != null) {
            NetworkMessage msg;
            while ((msg = client.getIncomingMessage()) != null) {
                if (msg.getType() == NetworkMessage.MessageType.START_GAME) {
                    setStatus("Game starting!", Color.GREEN);
                    fireGameStart();
                    return;
                }
                if (msg.getType() == NetworkMessage.MessageType.JOIN_RESPONSE && msg.getData() instanceof PlayerState ps) {
                    // For client mode, track player names from join responses
                    if (server == null) {
                        boolean found = false;
                        for (String n : lobbyPlayerNames) {
                            if (n.equals(ps.playerName)) { found = true; break; }
                        }
                        if (!found && ps.playerName != null) lobbyPlayerNames.add(ps.playerName);
                    }
                }
            }
        }
        repaint();
    }

    private void cleanupSession() {
        lobbyTimer.stop();
        if (client != null && client.isConnected()) client.disconnect();
        if (server != null && server.isRunning()) server.stop();
        client = null;
        server = null;
        gameStartTriggered = false;
        hostMode = false;
        lobbyPlayerNames.clear();
    }

    private void setStatus(String msg, Color color) {
        statusText = msg;
        statusColor = color;
    }

    // ===================== PUBLIC API =====================

    public void setOnGameStart(ActionListener l) { this.onGameStart = l; }
    public void setOnBackClick(ActionListener l) { this.onBackClick = l; }
    public GameServer getServer() { return server; }
    public void setServer(GameServer s) { this.server = s; }
    public GameClient getClient() { return client; }
    public void setClient(GameClient c) { this.client = c; }
    public boolean isHostMode() { return hostMode; }

    public void resetGame() {
        cleanupSession();
        nameFieldText = "Player";
        hostFieldText = NetworkConstants.SERVER_HOST;
        nameFieldFocused = false;
        hostFieldFocused = false;
        currentStep = Step.CHOICE;
        setStatus("Choose how you want to play", CYAN_DIM);
        repaint();
    }
}
