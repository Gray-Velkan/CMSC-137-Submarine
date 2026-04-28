package edu.cmsc137.submarine.core;

// item sitting on the floor
public class ItemEntity {
    private final double x;
    private final double y;
    private final ItemType itemType;

    public ItemEntity(double x, double y, ItemType itemType) {
        this.x = x;
        this.y = y;
        this.itemType = itemType == null ? ItemType.NONE : itemType;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public ItemType getItemType() {
        return itemType;
    }
}
