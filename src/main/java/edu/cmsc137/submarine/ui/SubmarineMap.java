package edu.cmsc137.submarine.ui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubmarineMap {
    private static final int WALL_THICKNESS = 12;

    private final Rectangle hullBounds;
    private final List<Rectangle> roomFloors;
    private final List<Rectangle> doorwayGaps;
    private final List<Rectangle> solidWalls;
    private final List<RoomLabel> roomLabels;

    public SubmarineMap() {
        this.hullBounds = new Rectangle(72, 116, 800, 380);
        this.roomFloors = new ArrayList<>();
        this.doorwayGaps = new ArrayList<>();
        this.solidWalls = new ArrayList<>();
        this.roomLabels = new ArrayList<>();

        buildRoomFloors();
        buildDoorways();
        buildWalls();
        buildLabels();
    }

    public Rectangle getHullBounds() {
        return new Rectangle(hullBounds);
    }

    public List<Rectangle> getRoomFloors() {
        return Collections.unmodifiableList(roomFloors);
    }

    public List<Rectangle> getDoorwayGaps() {
        return Collections.unmodifiableList(doorwayGaps);
    }

    public List<Rectangle> getSolidWalls() {
        return Collections.unmodifiableList(solidWalls);
    }

    public List<RoomLabel> getRoomLabels() {
        return Collections.unmodifiableList(roomLabels);
    }

    public boolean intersectsWall(Rectangle hitbox) {
        for (Rectangle wall : solidWalls) {
            if (hitbox.intersects(wall)) {
                return true;
            }
        }
        return false;
    }

    private void buildRoomFloors() {
        // command bridge
        roomFloors.add(new Rectangle(92, 136, 220, 118));

        // storage and airlock
        roomFloors.add(new Rectangle(348, 136, 228, 118));

        // central hallway
        roomFloors.add(new Rectangle(92, 272, 740, 60));

        // reactor room
        roomFloors.add(new Rectangle(348, 348, 228, 118));

        // engine room
        roomFloors.add(new Rectangle(612, 136, 220, 240));
    }

    private void buildDoorways() {
        doorwayGaps.add(new Rectangle(184, 254, 36, WALL_THICKNESS));
        doorwayGaps.add(new Rectangle(444, 254, 36, WALL_THICKNESS));
        doorwayGaps.add(new Rectangle(700, 254, 36, WALL_THICKNESS));
        doorwayGaps.add(new Rectangle(444, 336, 36, WALL_THICKNESS));
    }

    private void buildWalls() {
        // outer hull walls
        solidWalls.add(new Rectangle(hullBounds.x, hullBounds.y, hullBounds.width, WALL_THICKNESS));
        solidWalls.add(new Rectangle(hullBounds.x, hullBounds.y + hullBounds.height - WALL_THICKNESS, hullBounds.width, WALL_THICKNESS));
        solidWalls.add(new Rectangle(hullBounds.x, hullBounds.y, WALL_THICKNESS, hullBounds.height));
        solidWalls.add(new Rectangle(hullBounds.x + hullBounds.width - WALL_THICKNESS, hullBounds.y, WALL_THICKNESS, hullBounds.height));

        // internal vertical partitions
        solidWalls.add(new Rectangle(324, 136, WALL_THICKNESS, 118));
        solidWalls.add(new Rectangle(588, 136, WALL_THICKNESS, 118));

        // wall line between top rooms and central hallway with doorway gaps
        solidWalls.add(new Rectangle(92, 254, 92, WALL_THICKNESS));
        solidWalls.add(new Rectangle(220, 254, 224, WALL_THICKNESS));
        solidWalls.add(new Rectangle(480, 254, 220, WALL_THICKNESS));
        solidWalls.add(new Rectangle(736, 254, 96, WALL_THICKNESS));

        // reactor enclosure and doorway from hallway
        solidWalls.add(new Rectangle(348, 336, 96, WALL_THICKNESS));
        solidWalls.add(new Rectangle(480, 336, 96, WALL_THICKNESS));
        solidWalls.add(new Rectangle(348, 348, WALL_THICKNESS, 118));
        solidWalls.add(new Rectangle(564, 348, WALL_THICKNESS, 118));
        solidWalls.add(new Rectangle(348, 454, 228, WALL_THICKNESS));
    }

    private void buildLabels() {
        roomLabels.add(new RoomLabel("Command Bridge", 118, 160));
        roomLabels.add(new RoomLabel("Storage / Airlock", 372, 160));
        roomLabels.add(new RoomLabel("Central Hallway", 374, 302));
        roomLabels.add(new RoomLabel("Reactor Room", 396, 372));
        roomLabels.add(new RoomLabel("Engine Room", 658, 160));
    }

    public static final class RoomLabel {
        private final String name;
        private final int x;
        private final int y;

        private RoomLabel(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public String getName() {
            return name;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}
