package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.core.GameState;
import edu.cmsc137.submarine.input.InputHandler;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {
    public static final int PANEL_WIDTH = 960;
    public static final int PANEL_HEIGHT = 540;

    private static final int PLAYER_WIDTH = 28;
    private static final int PLAYER_HEIGHT = 28;
    private static final double PLAYER_SPEED_PX_PER_SEC = 190.0;

    private static final int TASK_STATION_X = 680;
    private static final int TASK_STATION_Y = 210;
    private static final int TASK_STATION_W = 72;
    private static final int TASK_STATION_H = 72;
    private static final double TASK_INTERACTION_RADIUS = 80.0;
    private static final int TASK_BUOYANCY_REWARD = 5;

    private static final double INITIAL_TIME_SECONDS = 180.0;
    private static final int TARGET_FPS = 60;

    private final GameState gameState;
    private final InputHandler inputHandler;

    private Thread gameThread;
    private volatile boolean running;

    public GamePanel() {
        this.gameState = new GameState(PANEL_WIDTH, PANEL_HEIGHT, INITIAL_TIME_SECONDS);
        this.inputHandler = new InputHandler();

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(16, 28, 40));
        setFocusable(true);
        addKeyListener(inputHandler);

        // start loop once panel is attached to a visible window
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                startGameLoop();
                requestFocusInWindow();
            }
        });
    }

    public GameState getGameState() {
        return gameState;
    }

    public void startGameLoop() {
        if (running) {
            return;
        }
        running = true;
        gameThread = new Thread(this, "game-loop-thread");
        gameThread.start();
    }

    public void stopGameLoop() {
        running = false;
    }

    @Override
    public void run() {
        // fixed update step keeps simulation stable for future server sync
        final double fixedDeltaSeconds = 1.0 / TARGET_FPS;
        // target frame pacing for render loop
        final long nanosPerFrame = 1_000_000_000L / TARGET_FPS;

        long previousTime = System.nanoTime();
        double accumulator = 0.0;

        while (running) {
            long currentTime = System.nanoTime();
            long elapsedNanos = currentTime - previousTime;
            previousTime = currentTime;

            // accumulate elapsed time and consume it in fixed-size updates
            accumulator += elapsedNanos / 1_000_000_000.0;

            // fixed-step simulation keeps gameplay deterministic and server-ready
            while (accumulator >= fixedDeltaSeconds) {
                processInput(fixedDeltaSeconds);
                updateState(fixedDeltaSeconds);
                accumulator -= fixedDeltaSeconds;
            }

            repaint();

            // basic frame pacing
            long frameTime = System.nanoTime() - currentTime;
            long sleepNanos = nanosPerFrame - frameTime;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }

    private void processInput(double deltaSeconds) {
        if (gameState.isGameOver()) {
            return;
        }

        double dx = 0.0;
        double dy = 0.0;
        // convert keyboard state into movement intent
        if (inputHandler.isUpPressed()) {
            dy -= 1.0;
        }
        if (inputHandler.isDownPressed()) {
            dy += 1.0;
        }
        if (inputHandler.isLeftPressed()) {
            dx -= 1.0;
        }
        if (inputHandler.isRightPressed()) {
            dx += 1.0;
        }

        // normalize diagonal movement
        if (dx != 0.0 || dy != 0.0) {
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }

        gameState.movePlayer(
                dx * PLAYER_SPEED_PX_PER_SEC * deltaSeconds,
                dy * PLAYER_SPEED_PX_PER_SEC * deltaSeconds,
                PLAYER_WIDTH,
                PLAYER_HEIGHT
        );

        // apply task reward when interact is queued and player is in range
        if (inputHandler.consumeInteract() && canInteractWithTaskStation()) {
            gameState.addBuoyancy(TASK_BUOYANCY_REWARD);
        }
    }

    private void updateState(double deltaSeconds) {
        // keep all simulation progression inside game state
        gameState.update(deltaSeconds);
    }

    private boolean canInteractWithTaskStation() {
        double stationCenterX = TASK_STATION_X + TASK_STATION_W * 0.5;
        double stationCenterY = TASK_STATION_Y + TASK_STATION_H * 0.5;
        return gameState.isNearTaskStation(stationCenterX, stationCenterY, TASK_INTERACTION_RADIUS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSubmarineRoom(g2);
        drawTaskStation(g2);
        drawPlayer(g2);
        drawHud(g2);

        if (gameState.isGameOver()) {
            drawGameOverOverlay(g2);
        }

        g2.dispose();
    }

    private void drawSubmarineRoom(Graphics2D g2) {
        g2.setColor(new Color(26, 42, 56));
        g2.fillRoundRect(20, 20, PANEL_WIDTH - 40, PANEL_HEIGHT - 40, 24, 24);

        g2.setColor(new Color(56, 88, 108));
        g2.drawRoundRect(20, 20, PANEL_WIDTH - 40, PANEL_HEIGHT - 40, 24, 24);

        // draw floor guide lines so movement is easier to read
        g2.setColor(new Color(36, 56, 74));
        for (int y = 60; y < PANEL_HEIGHT - 40; y += 40) {
            g2.drawLine(40, y, PANEL_WIDTH - 40, y);
        }
    }

    private void drawTaskStation(Graphics2D g2) {
        boolean near = canInteractWithTaskStation();
        Color base = near ? new Color(88, 192, 120) : new Color(210, 156, 78);

        g2.setColor(base);
        g2.fill(new RoundRectangle2D.Double(
                TASK_STATION_X, TASK_STATION_Y, TASK_STATION_W, TASK_STATION_H, 14, 14
        ));

        g2.setColor(new Color(16, 20, 26));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g2.drawString("TASK", TASK_STATION_X + 16, TASK_STATION_Y + 40);
        if (near) {
            // show prompt only when player can interact
            g2.drawString("Press E", TASK_STATION_X - 6, TASK_STATION_Y + TASK_STATION_H + 18);
        }
    }

    private void drawPlayer(Graphics2D g2) {
        int px = (int) Math.round(gameState.getPlayerX());
        int py = (int) Math.round(gameState.getPlayerY());

        g2.setColor(new Color(72, 176, 255));
        g2.fillRoundRect(px, py, PLAYER_WIDTH, PLAYER_HEIGHT, 8, 8);

        g2.setColor(new Color(14, 24, 34));
        g2.drawRoundRect(px, py, PLAYER_WIDTH, PLAYER_HEIGHT, 8, 8);
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(new Color(240, 245, 250));
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));

        String buoyancyText = "Buoyancy: " + gameState.getBuoyancy();
        String timeText = String.format("Time: %.1fs", gameState.getTimeRemainingSeconds());
        String itemText = "Held Item: " + gameState.getHeldItem();

        g2.drawString(buoyancyText, 36, 34);
        g2.drawString(timeText, 230, 34);
        g2.drawString(itemText, 390, 34);
        g2.drawString("WASD Move | E Interact", 640, 34);
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        g2.drawString("TIME UP", PANEL_WIDTH / 2 - 100, PANEL_HEIGHT / 2 - 10);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        g2.drawString(
                "Final Buoyancy: " + gameState.getBuoyancy(),
                PANEL_WIDTH / 2 - 90,
                PANEL_HEIGHT / 2 + 28
        );
    }
}
