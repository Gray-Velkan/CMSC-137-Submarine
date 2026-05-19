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
    private double totalElapsedTime;
    private boolean powerRoutedToPump;

    // local player state
    private final Player player;

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
        this.totalElapsedTime = 0.0;
        this.powerRoutedToPump = false;

        // spawn roughly at the center
        this.player = new Player(worldWidth * 0.5, worldHeight * 0.5, 0.0);
    }

    public void update(double deltaSeconds) {
        if (isRoundOver()) {
            return;
        }

        // countdown cannot go below zero
        timeRemainingSeconds = Math.max(0.0, timeRemainingSeconds - deltaSeconds);

        // Track total elapsed time for difficulty scaling
        totalElapsedTime += deltaSeconds;

        // Progressive difficulty: increase passive buoyancy drain rate by 10% for every 30 seconds elapsed
        int intervals = (int) (totalElapsedTime / 30.0);
        double currentDrainRate = buoyancyDrainPerSecond * Math.pow(1.10, intervals);

        // apply progressive passive flooding over time
        buoyancyDrainAccumulator += currentDrainRate * deltaSeconds;
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
        double nextX = player.getX() + dx;
        double nextY = player.getY() + dy;

        // clamp player to world bounds
        double maxX = Math.max(0, worldWidth - playerWidth);
        double maxY = Math.max(0, worldHeight - playerHeight);
        player.setPosition(clamp(nextX, 0.0, maxX), clamp(nextY, 0.0, maxY));
    }

    public boolean isNearTaskStation(double stationCenterX, double stationCenterY, double interactionRadius) {
        // distance check uses squared values to avoid sqrt cost
        double dx = player.getX() - stationCenterX;
        double dy = player.getY() - stationCenterY;
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

    public void setBuoyancy(int buoyancy) {
        this.buoyancy = Math.max(0, Math.min(buoyancyTarget, buoyancy));
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

    public void setTimeRemainingSeconds(double timeRemainingSeconds) {
        this.timeRemainingSeconds = Math.max(0.0, timeRemainingSeconds);
    }

    public boolean isSank() {
        return sank;
    }

    public void setSank(boolean sank) {
        this.sank = sank;
    }

    public double getPlayerX() {
        return player.getX();
    }

    public double getPlayerY() {
        return player.getY();
    }

    public String getHeldItem() {
        return player.getCurrentItem().name();
    }

    public void setHeldItem(String heldItem) {
        this.player.setCurrentItem(parseItemType(heldItem));
    }

    public void setPlayerPosition(double x, double y) {
        this.player.setPosition(clamp(x, 0.0, worldWidth), clamp(y, 0.0, worldHeight));
    }

    public Rectangle getPlayerHitbox(int playerWidth, int playerHeight) {
        return new Rectangle(
                (int) Math.round(player.getX()),
                (int) Math.round(player.getY()),
                playerWidth,
                playerHeight
        );
    }

    public Rectangle getProjectedPlayerHitbox(double dx, double dy, int playerWidth, int playerHeight) {
        return new Rectangle(
                (int) Math.round(player.getX() + dx),
                (int) Math.round(player.getY() + dy),
                playerWidth,
                playerHeight
        );
    }

    public Player getPlayer() {
        return player;
    }

    public ItemEntity tossHeldItem() {
        return player.tossItem();
    }

    public ItemType getHeldItemType() {
        return player.getCurrentItem();
    }

    public void setHeldItemType(ItemType itemType) {
        player.setCurrentItem(itemType);
    }

    public boolean isPumping() {
        return player.isPumping();
    }

    public void setPumping(boolean pumping) {
        player.setPumping(pumping);
    }

    public double getTotalElapsedTime() {
        return totalElapsedTime;
    }

    public void setTotalElapsedTime(double totalElapsedTime) {
        this.totalElapsedTime = totalElapsedTime;
    }

    public boolean isPowerRoutedToPump() {
        return powerRoutedToPump;
    }

    public void setPowerRoutedToPump(boolean powerRoutedToPump) {
        this.powerRoutedToPump = powerRoutedToPump;
    }

    private ItemType parseItemType(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return ItemType.NONE;
        }

        try {
            return ItemType.valueOf(itemName);
        } catch (IllegalArgumentException ex) {
            return ItemType.NONE;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
