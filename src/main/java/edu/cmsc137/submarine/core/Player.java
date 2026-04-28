package edu.cmsc137.submarine.core;

import java.awt.Rectangle;

// player state and movement logic only
public class Player {
    private static final double PLATE_SPEED_MULTIPLIER = 0.75;
    private static final int TOSS_DISTANCE_PIXELS = 64;

    private double x;
    private double y;
    private int facingX;
    private int facingY;
    private double baseSpeed;
    private ItemType currentItem;
    private boolean pumping;

    public Player(double x, double y, double baseSpeed) {
        this.x = x;
        this.y = y;
        this.baseSpeed = baseSpeed;
        this.currentItem = ItemType.NONE;
        this.facingX = 1;
        this.facingY = 0;
        this.pumping = false;
    }

    public void move(double dx, double dy) {
        x += dx;
        y += dy;

        if (dx > 0) {
            facingX = 1;
            facingY = 0;
        } else if (dx < 0) {
            facingX = -1;
            facingY = 0;
        } else if (dy > 0) {
            facingX = 0;
            facingY = 1;
        } else if (dy < 0) {
            facingX = 0;
            facingY = -1;
        }
    }

    public double getMovementSpeed() {
        if (pumping) {
            return 0.0;
        }
        if (currentItem == ItemType.PATCH_PLATE) {
            return baseSpeed * PLATE_SPEED_MULTIPLIER;
        }
        return baseSpeed;
    }

    public ItemEntity tossItem() {
        if (currentItem == ItemType.NONE) {
            return null;
        }

        double dropX = x + (facingX * TOSS_DISTANCE_PIXELS);
        double dropY = y + (facingY * TOSS_DISTANCE_PIXELS);
        ItemEntity tossedItem = new ItemEntity(dropX, dropY, currentItem);
        currentItem = ItemType.NONE;
        return tossedItem;
    }

    public Rectangle getHitbox(int width, int height) {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public ItemType getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(ItemType currentItem) {
        this.currentItem = currentItem == null ? ItemType.NONE : currentItem;
    }

    public boolean isPumping() {
        return pumping;
    }

    public void setPumping(boolean pumping) {
        this.pumping = pumping;
    }

    public int getFacingX() {
        return facingX;
    }

    public int getFacingY() {
        return facingY;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public void setBaseSpeed(double baseSpeed) {
        this.baseSpeed = baseSpeed;
    }
}
