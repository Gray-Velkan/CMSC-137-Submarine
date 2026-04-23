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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {
    public static final int PANEL_WIDTH = 960;
    public static final int PANEL_HEIGHT = 540;

    private static final int PLAYER_WIDTH = 28;
    private static final int PLAYER_HEIGHT = 28;
    private static final double PLAYER_SPEED_PX_PER_SEC = 190.0;

    private static final int TASK_STATION_W = 78;
    private static final int TASK_STATION_H = 78;
    private static final double TASK_INTERACTION_RADIUS = 80.0;

    private static final int ITEM_SIZE = 18;
    private static final double ITEM_PICKUP_RADIUS = 56.0;
    private static final String NO_ITEM = "None";

    private static final double INITIAL_TIME_SECONDS = 180.0;
    private static final int TARGET_FPS = 60;

    private final GameState gameState;
    private final InputHandler inputHandler;
    private final List<TaskStation> taskStations;
    private final List<ItemEntity> worldItems;

    private Thread gameThread;
    private volatile boolean running;

    public GamePanel() {
        this.gameState = new GameState(PANEL_WIDTH, PANEL_HEIGHT, INITIAL_TIME_SECONDS);
        this.inputHandler = new InputHandler();
        this.taskStations = createTaskStations();
        this.worldItems = createInitialItems();

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
        if (gameState.isRoundOver()) {
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

        // drop held item to current position
        if (inputHandler.consumeDrop()) {
            dropHeldItemAtPlayer();
        }

        // interact tries pickup first, then station use
        if (inputHandler.consumeInteract()) {
            handleInteraction();
        }
    }

    private void updateState(double deltaSeconds) {
        // keep all simulation progression inside game state
        gameState.update(deltaSeconds);
    }

    private void handleInteraction() {
        if (tryPickupNearbyItem()) {
            return;
        }
        tryUseNearbyTaskStation();
    }

    private boolean tryPickupNearbyItem() {
        if (isHoldingItem()) {
            return false;
        }

        int index = findNearbyItemIndex();
        if (index < 0) {
            return false;
        }

        ItemEntity pickedItem = worldItems.remove(index);
        gameState.setHeldItem(pickedItem.name);
        return true;
    }

    private void tryUseNearbyTaskStation() {
        TaskStation station = findNearbyTaskStation();
        if (station == null) {
            return;
        }

        String held = gameState.getHeldItem();
        if (station.requiredItem.equals(held)) {
            gameState.addBuoyancy(station.buoyancyReward);
            gameState.setHeldItem(NO_ITEM);
        }
    }

    private void dropHeldItemAtPlayer() {
        if (!isHoldingItem()) {
            return;
        }

        int px = (int) Math.round(gameState.getPlayerX() + PLAYER_WIDTH * 0.5 - ITEM_SIZE * 0.5);
        int py = (int) Math.round(gameState.getPlayerY() + PLAYER_HEIGHT * 0.5 - ITEM_SIZE * 0.5);
        worldItems.add(new ItemEntity(px, py, gameState.getHeldItem()));
        gameState.setHeldItem(NO_ITEM);
    }

    private boolean isHoldingItem() {
        return !NO_ITEM.equals(gameState.getHeldItem());
    }

    private int findNearbyItemIndex() {
        for (int i = 0; i < worldItems.size(); i++) {
            ItemEntity item = worldItems.get(i);
            double itemCenterX = item.x + ITEM_SIZE * 0.5;
            double itemCenterY = item.y + ITEM_SIZE * 0.5;
            if (gameState.isNearTaskStation(itemCenterX, itemCenterY, ITEM_PICKUP_RADIUS)) {
                return i;
            }
        }
        return -1;
    }

    private TaskStation findNearbyTaskStation() {
        for (TaskStation station : taskStations) {
            if (gameState.isNearTaskStation(station.centerX(), station.centerY(), TASK_INTERACTION_RADIUS)) {
                return station;
            }
        }
        return null;
    }

    private List<TaskStation> createTaskStations() {
        List<TaskStation> stations = new ArrayList<>();
        stations.add(new TaskStation("Engine Console", 680, 100, 18, "Wrench"));
        stations.add(new TaskStation("Ballast Controls", 730, 350, 20, "Sealant"));
        stations.add(new TaskStation("Reactor Switchboard", 190, 300, 15, "Battery"));
        return stations;
    }

    private List<ItemEntity> createInitialItems() {
        List<ItemEntity> items = new ArrayList<>();
        items.add(new ItemEntity(120, 110, "Wrench"));
        items.add(new ItemEntity(420, 220, "Sealant"));
        items.add(new ItemEntity(260, 430, "Battery"));
        return items;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSubmarineRoom(g2);
        drawTaskStations(g2);
        drawItems(g2);
        drawPlayer(g2);
        drawHud(g2);

        if (gameState.isRoundOver()) {
            drawRoundEndOverlay(g2);
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

    private void drawTaskStations(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        for (TaskStation station : taskStations) {
            boolean near = gameState.isNearTaskStation(station.centerX(), station.centerY(), TASK_INTERACTION_RADIUS);
            Color base = near ? new Color(88, 192, 120) : new Color(210, 156, 78);

            g2.setColor(base);
            g2.fill(new RoundRectangle2D.Double(
                    station.x, station.y, TASK_STATION_W, TASK_STATION_H, 14, 14
            ));

            g2.setColor(new Color(16, 20, 26));
            g2.drawString("TASK", station.x + 21, station.y + 24);
            g2.drawString(station.requiredItem, station.x + 8, station.y + 40);
            g2.drawString("+" + station.buoyancyReward, station.x + 25, station.y + 56);

            if (near) {
                // show prompt only when player can interact
                g2.drawString("Press E", station.x + 10, station.y + TASK_STATION_H + 16);
            }
        }
    }

    private void drawItems(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        for (ItemEntity item : worldItems) {
            boolean near = gameState.isNearTaskStation(
                    item.x + ITEM_SIZE * 0.5,
                    item.y + ITEM_SIZE * 0.5,
                    ITEM_PICKUP_RADIUS
            );

            g2.setColor(near ? new Color(255, 235, 122) : new Color(196, 208, 218));
            g2.fillOval(item.x, item.y, ITEM_SIZE, ITEM_SIZE);

            g2.setColor(new Color(20, 28, 34));
            g2.drawString(item.name, item.x - 8, item.y - 4);

            if (near && !isHoldingItem()) {
                g2.drawString("Press E", item.x - 2, item.y + ITEM_SIZE + 14);
            }
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
        String objectiveText = "Objective: Reach " + gameState.getBuoyancyTarget() + " buoyancy before sinking";
        String timeText = String.format("Time: %.1fs", gameState.getTimeRemainingSeconds());
        String itemText = "Held Item: " + gameState.getHeldItem();
        String drainText = String.format("Drain: %.1f/s", gameState.getBuoyancyDrainPerSecond());

        g2.drawString(buoyancyText, 36, 34);
        g2.drawString(timeText, 230, 34);
        g2.drawString(drainText, 380, 34);
        g2.drawString(itemText, 500, 34);
        g2.drawString("WASD Move | E Interact | Q Drop", 36, 56);
        g2.drawString(objectiveText, 36, 78);
    }

    private void drawRoundEndOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));

        String title;
        if (gameState.hasWon()) {
            title = "MISSION SAVED";
        } else if (gameState.hasLostBySinking()) {
            title = "SUBMARINE SUNK";
        } else {
            title = "TIME UP";
        }
        g2.drawString(title, PANEL_WIDTH / 2 - 170, PANEL_HEIGHT / 2 - 10);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        g2.drawString(
                "Final Buoyancy: " + gameState.getBuoyancy(),
                PANEL_WIDTH / 2 - 90,
                PANEL_HEIGHT / 2 + 28
        );
    }

    private static final class TaskStation {
        private final String name;
        private final int x;
        private final int y;
        private final int buoyancyReward;
        private final String requiredItem;

        private TaskStation(String name, int x, int y, int buoyancyReward, String requiredItem) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.buoyancyReward = buoyancyReward;
            this.requiredItem = requiredItem;
        }

        private double centerX() {
            return x + TASK_STATION_W * 0.5;
        }

        private double centerY() {
            return y + TASK_STATION_H * 0.5;
        }
    }

    private static final class ItemEntity {
        private final int x;
        private final int y;
        private final String name;

        private ItemEntity(int x, int y, String name) {
            this.x = x;
            this.y = y;
            this.name = name;
        }
    }
}
