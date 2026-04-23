package edu.cmsc137.submarine.ui;

import edu.cmsc137.submarine.core.GameState;
import edu.cmsc137.submarine.input.InputHandler;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {
    public static final int PANEL_WIDTH = 1280;
    public static final int PANEL_HEIGHT = 768;

    private static final int PLAYER_WIDTH = 26;
    private static final int PLAYER_HEIGHT = 26;
    private static final double PLAYER_SPEED_PX_PER_SEC = 190.0;

    private static final int TASK_STATION_W = 72;
    private static final int TASK_STATION_H = 72;
    private static final double TASK_INTERACTION_RADIUS = 78.0;

    private static final int ITEM_SIZE = 16;
    private static final double ITEM_PICKUP_RADIUS = 52.0;
    private static final String NO_ITEM = "None";

    private static final double INITIAL_TIME_SECONDS = 180.0;
    private static final int TARGET_FPS = 60;

    private final TileManager tileManager;
    private final GameState gameState;
    private final InputHandler inputHandler;
    private final List<TaskStation> taskStations;
    private final List<ItemEntity> worldItems;

    private Thread gameThread;
    private volatile boolean running;

    public GamePanel() {
        this.tileManager = new TileManager();
        this.gameState = new GameState(
                tileManager.getMapWidthPixels(),
                tileManager.getMapHeightPixels(),
                INITIAL_TIME_SECONDS
        );
        this.inputHandler = new InputHandler();
        this.taskStations = createTaskStations();
        this.worldItems = createInitialItems();

        // place player on central hallway floor tile
        gameState.setPlayerPosition(tileToPixel(12) + 4, tileToPixel(13) + 3);

        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(7, 14, 22));
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

        double moveX = dx * PLAYER_SPEED_PX_PER_SEC * deltaSeconds;
        double moveY = dy * PLAYER_SPEED_PX_PER_SEC * deltaSeconds;
        movePlayerWithTileCollision(moveX, moveY);

        // drop held item to current position
        if (inputHandler.consumeDrop()) {
            dropHeldItemAtPlayer();
        }

        // interact tries pickup first, then station use
        if (inputHandler.consumeInteract()) {
            handleInteraction();
        }
    }

    private void movePlayerWithTileCollision(double moveX, double moveY) {
        // x-axis projection for AABB collision
        Rectangle projectedX = gameState.getProjectedPlayerHitbox(moveX, 0.0, PLAYER_WIDTH, PLAYER_HEIGHT);
        if (!tileManager.isSolidHitbox(projectedX)) {
            gameState.movePlayer(moveX, 0.0, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        // y-axis projection for AABB collision and wall sliding
        Rectangle projectedY = gameState.getProjectedPlayerHitbox(0.0, moveY, PLAYER_WIDTH, PLAYER_HEIGHT);
        if (!tileManager.isSolidHitbox(projectedY)) {
            gameState.movePlayer(0.0, moveY, PLAYER_WIDTH, PLAYER_HEIGHT);
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
        // command bridge
        stations.add(new TaskStation("Nav Console", tileToPixel(5), tileToPixel(3), 18, "Sealant"));
        // reactor room
        stations.add(new TaskStation("Reactor Core", tileToPixel(22), tileToPixel(13), 20, "Battery"));
        // engine room
        stations.add(new TaskStation("Engine Console", tileToPixel(31), tileToPixel(8), 15, "Wrench"));
        return stations;
    }

    private List<ItemEntity> createInitialItems() {
        List<ItemEntity> items = new ArrayList<>();
        // storage / airlock room
        items.add(new ItemEntity(tileToPixel(20), tileToPixel(4), "Wrench"));
        items.add(new ItemEntity(tileToPixel(22), tileToPixel(4), "Sealant"));
        // reactor room
        items.add(new ItemEntity(tileToPixel(20), tileToPixel(14), "Battery"));
        return items;
    }

    private int tileToPixel(int tile) {
        return tile * TileManager.TILE_SIZE;
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
        tileManager.draw(g2);
    }

    private void drawTaskStations(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        for (TaskStation station : taskStations) {
            boolean near = gameState.isNearTaskStation(station.centerX(), station.centerY(), TASK_INTERACTION_RADIUS);
            Color base = near ? new Color(88, 192, 120) : new Color(208, 154, 84);

            g2.setColor(base);
            g2.fill(new RoundRectangle2D.Double(
                    station.x, station.y, TASK_STATION_W, TASK_STATION_H, 12, 12
            ));

            g2.setColor(new Color(12, 18, 26));
            g2.drawString("TASK", station.x + 22, station.y + 22);
            g2.drawString(station.requiredItem, station.x + 8, station.y + 40);
            g2.drawString("+" + station.buoyancyReward, station.x + 24, station.y + 56);

            if (near) {
                g2.drawString("Press E", station.x + 8, station.y + TASK_STATION_H + 15);
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

            g2.setColor(near ? new Color(255, 231, 112) : new Color(192, 208, 224));
            g2.fillOval(item.x, item.y, ITEM_SIZE, ITEM_SIZE);

            g2.setColor(new Color(16, 22, 30));
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
        int cardX = 20;
        int cardY = 14;
        int cardW = 980;
        int cardH = 72;

        g2.setPaint(new GradientPaint(cardX, cardY, new Color(9, 16, 28, 228), cardX, cardY + cardH, new Color(6, 12, 22, 214)));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 18, 18);
        g2.setColor(new Color(90, 123, 152));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 18, 18);

        g2.setColor(new Color(227, 239, 250));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g2.drawString("Buoyancy", cardX + 18, cardY + 22);
        g2.drawString("Time", cardX + 196, cardY + 22);
        g2.drawString("Drain", cardX + 312, cardY + 22);
        g2.drawString("Held Item", cardX + 438, cardY + 22);

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
        g2.drawString(String.valueOf(gameState.getBuoyancy()), cardX + 18, cardY + 46);
        g2.drawString(String.format("%.1fs", gameState.getTimeRemainingSeconds()), cardX + 196, cardY + 46);
        g2.drawString(String.format("%.1f/s", gameState.getBuoyancyDrainPerSecond()), cardX + 312, cardY + 46);
        g2.drawString(gameState.getHeldItem(), cardX + 438, cardY + 46);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.setColor(new Color(171, 203, 225));
        g2.drawString("WASD move   E interact/pickup   Q drop", cardX + 18, cardY + 63);
        g2.drawString("Objective: Reach " + gameState.getBuoyancyTarget() + " buoyancy before sinking", cardX + 350, cardY + 63);

        drawDepthGauge(g2, 1020, 14, 240, 72);
    }

    private void drawDepthGauge(Graphics2D g2, int x, int y, int w, int h) {
        double ratio = Math.max(0.0, Math.min(1.0, gameState.getBuoyancy() / (double) gameState.getBuoyancyTarget()));
        int innerX = x + 8;
        int innerY = y + 28;
        int innerW = w - 16;
        int innerH = h - 36;

        g2.setColor(new Color(8, 14, 24, 228));
        g2.fillRoundRect(x, y, w, h, 16, 16);
        g2.setColor(new Color(90, 123, 152));
        g2.drawRoundRect(x, y, w, h, 16, 16);

        g2.setColor(new Color(43, 58, 75));
        g2.fillRoundRect(innerX, innerY, innerW, innerH, 8, 8);

        int fillW = (int) Math.round(innerW * ratio);
        if (fillW > 0) {
            g2.setClip(innerX, innerY, fillW, innerH);
            g2.setPaint(new GradientPaint(innerX, innerY + innerH, new Color(27, 99, 168), innerX, innerY, new Color(101, 208, 255)));
            g2.fillRoundRect(innerX, innerY, innerW, innerH, 8, 8);
            g2.setClip(null);
        }

        g2.setColor(new Color(226, 239, 250));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g2.drawString("Depth", x + 10, y + 15);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        int depthMeters = (int) Math.round((1.0 - ratio) * 300.0);
        g2.drawString(depthMeters + " m", x + 10, y + h - 8);
        g2.drawString("Surface", x + w - 58, y + h - 8);
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
        g2.drawString(title, PANEL_WIDTH / 2 - 180, PANEL_HEIGHT / 2 - 10);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        g2.drawString(
                "Final Buoyancy: " + gameState.getBuoyancy(),
                PANEL_WIDTH / 2 - 96,
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
