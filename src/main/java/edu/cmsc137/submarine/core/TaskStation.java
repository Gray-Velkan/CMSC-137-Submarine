package edu.cmsc137.submarine.core;

import java.io.Serializable;

/**
 * Represents a task station in the submarine.
 * Handles anti-spam cooldown logic and position checking.
 */
public class TaskStation implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String name;
    public final int x;
    public final int y;
    public final int buoyancyReward;
    public final ItemType requiredItem;

    // Cooldown state for anti-spam mechanics
    private long lastUsedTime = 0;
    private final long cooldownDuration = 15000; // 15 seconds (15000 milliseconds)

    public TaskStation(String name, int x, int y, int buoyancyReward, ItemType requiredItem) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.buoyancyReward = buoyancyReward;
        this.requiredItem = requiredItem == null ? ItemType.NONE : requiredItem;
    }

    public double centerX() {
        return x + 72 * 0.5; // TASK_STATION_W = 72
    }

    public double centerY() {
        return y + 72 * 0.5; // TASK_STATION_H = 72
    }

    /**
     * Checks if the task is currently interactable (not on cooldown).
     */
    public boolean isInteractable() {
        return (System.currentTimeMillis() - lastUsedTime) >= cooldownDuration;
    }

    /**
     * Attempts to interact with the task. Returns true if successful and enters cooldown.
     */
    public boolean interact() {
        if (!isInteractable()) {
            return false;
        }
        lastUsedTime = System.currentTimeMillis();
        return true;
    }

    public long getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(long lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public long getCooldownDuration() {
        return cooldownDuration;
    }
}
