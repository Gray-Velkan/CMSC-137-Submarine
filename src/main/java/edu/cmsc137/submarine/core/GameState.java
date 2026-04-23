package edu.cmsc137.submarine.core;

import java.awt.Rectangle;

// pure game state container with no rendering or input dependencies
public class GameState {
    // world bounds for movement clamping
    private final int worldWidth;
    private final int worldHeight;

    // submarine-wide state
    private int buoyancy;
    private double timeRemainingSeconds;
    private final int buoyancyTarget;
    private final double buoyancyDrainPerSecond;
    private boolean sank;
    private double buoyancyDrainAccumulator;

    // local player state
    private double playerX;
    private double playerY;
    private String heldItem;

    public GameState(int worldWidth, int worldHeight, double initialTimeSeconds) {
        this(worldWidth, worldHeight, initialTimeSeconds, 50, 100, 1.2);
    }

    public GameState(
            int worldWidth,
            int worldHeight,
            double initialTimeSeconds,
            int initialBuoyancy,
            int buoyancyTarget,
            double buoyancyDrainPerSecond
    ) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.buoyancy = Math.max(0, initialBuoyancy);
        this.timeRemainingSeconds = initialTimeSeconds;
        this.buoyancyTarget = Math.max(1, buoyancyTarget);
        this.buoyancyDrainPerSecond = Math.max(0.0, buoyancyDrainPerSecond);
        this.sank = false;
        this.buoyancyDrainAccumulator = 0.0;

        // spawn roughly at the center
        this.playerX = worldWidth * 0.5;
        this.playerY = worldHeight * 0.5;
        this.heldItem = "None";
    }

    public void update(double deltaSeconds) {
        if (isRoundOver()) {
            return;
        }

        // countdown cannot go below zero
        timeRemainingSeconds = Math.max(0.0, timeRemainingSeconds - deltaSeconds);

        // apply passive flooding over time
        buoyancyDrainAccumulator += buoyancyDrainPerSecond * deltaSeconds;
        int drained = (int) buoyancyDrainAccumulator;
        if (drained > 0) {
            buoyancy = Math.max(0, buoyancy - drained);
            buoyancyDrainAccumulator -= drained;
        }

        if (buoyancy <= 0) {
            sank = true;
        }
    }

    public void movePlayer(double dx, double dy, int playerWidth, int playerHeight) {
        // apply movement first
        playerX += dx;
        playerY += dy;

        // clamp player to world bounds
        double maxX = worldWidth - playerWidth;
        double maxY = worldHeight - playerHeight;
        playerX = clamp(playerX, 0.0, maxX);
        playerY = clamp(playerY, 0.0, maxY);
    }

    public boolean isNearTaskStation(double stationCenterX, double stationCenterY, double interactionRadius) {
        // distance check uses squared values to avoid sqrt cost
        double playerCenterX = playerX;
        double playerCenterY = playerY;
        double dx = playerCenterX - stationCenterX;
        double dy = playerCenterY - stationCenterY;
        return (dx * dx + dy * dy) <= (interactionRadius * interactionRadius);
    }

    public void addBuoyancy(int amount) {
        // ignore negative rewards
        buoyancy = Math.min(buoyancyTarget, buoyancy + Math.max(0, amount));
    }

    public boolean isGameOver() {
        return isRoundOver();
    }

    public boolean hasWon() {
        return buoyancy >= buoyancyTarget;
    }

    public boolean hasLostBySinking() {
        return sank;
    }

    public boolean hasLostByTime() {
        return timeRemainingSeconds <= 0.0 && !hasWon();
    }

    public boolean isRoundOver() {
        return hasWon() || hasLostBySinking() || hasLostByTime();
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public int getBuoyancy() {
        return buoyancy;
    }

    public int getBuoyancyTarget() {
        return buoyancyTarget;
    }

    public double getBuoyancyDrainPerSecond() {
        return buoyancyDrainPerSecond;
    }

    public double getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public double getPlayerX() {
        return playerX;
    }

    public double getPlayerY() {
        return playerY;
    }

    public String getHeldItem() {
        return heldItem;
    }

    public void setHeldItem(String heldItem) {
        this.heldItem = heldItem;
    }

    public void setPlayerPosition(double x, double y) {
        this.playerX = clamp(x, 0.0, worldWidth);
        this.playerY = clamp(y, 0.0, worldHeight);
    }

    public Rectangle getPlayerHitbox(int playerWidth, int playerHeight) {
        return new Rectangle(
                (int) Math.round(playerX),
                (int) Math.round(playerY),
                playerWidth,
                playerHeight
        );
    }

    public Rectangle getProjectedPlayerHitbox(double dx, double dy, int playerWidth, int playerHeight) {
        return new Rectangle(
                (int) Math.round(playerX + dx),
                (int) Math.round(playerY + dy),
                playerWidth,
                playerHeight
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
