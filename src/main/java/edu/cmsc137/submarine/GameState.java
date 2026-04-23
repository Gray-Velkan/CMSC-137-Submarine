package edu.cmsc137.submarine;

// pure game state container with no rendering or input dependencies
public class GameState {
    // world bounds for movement clamping
    private final int worldWidth;
    private final int worldHeight;

    // submarine-wide state
    private int buoyancy;
    private double timeRemainingSeconds;

    // local player state
    private double playerX;
    private double playerY;
    private String heldItem;

    public GameState(int worldWidth, int worldHeight, double initialTimeSeconds) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.buoyancy = 0;
        this.timeRemainingSeconds = initialTimeSeconds;

        // spawn roughly at the center
        this.playerX = worldWidth * 0.5;
        this.playerY = worldHeight * 0.5;
        this.heldItem = "None";
    }

    public void update(double deltaSeconds) {
        // countdown cannot go below zero
        timeRemainingSeconds = Math.max(0.0, timeRemainingSeconds - deltaSeconds);
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
        buoyancy += Math.max(0, amount);
    }

    public boolean isGameOver() {
        return timeRemainingSeconds <= 0.0;
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
