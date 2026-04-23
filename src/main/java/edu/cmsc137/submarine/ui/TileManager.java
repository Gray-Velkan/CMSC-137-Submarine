package edu.cmsc137.submarine.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class TileManager {
    public static final int TILE_SIZE = 32;
    public static final int TILE_FLOOR = 0;
    public static final int TILE_WALL = 1;
    public static final int TILE_VOID = 2;

    private final BufferedImage[] tiles;
    private final int[][] mapGrid;

    public TileManager() {
        this.tiles = new BufferedImage[3];
        this.mapGrid = createSampleSubmarineGrid();
        loadAndSliceTiles("/tiles/submarine_tiles.png");
    }

    public void draw(Graphics2D g2d) {
        for (int row = 0; row < mapGrid.length; row++) {
            for (int col = 0; col < mapGrid[row].length; col++) {
                int tileId = mapGrid[row][col];
                if (tileId < 0 || tileId >= tiles.length || tiles[tileId] == null) {
                    continue;
                }

                int screenX = col * TILE_SIZE;
                int screenY = row * TILE_SIZE;
                g2d.drawImage(tiles[tileId], screenX, screenY, TILE_SIZE, TILE_SIZE, null);
            }
        }
    }

    public boolean isSolidTile(int row, int col) {
        if (!isInsideGrid(row, col)) {
            return true;
        }
        return mapGrid[row][col] == TILE_WALL;
    }

    public boolean isSolidAtPixel(int pixelX, int pixelY) {
        int col = worldToColumn(pixelX);
        int row = worldToRow(pixelY);
        return isSolidTile(row, col);
    }

    public boolean isSolidHitbox(Rectangle hitbox) {
        int left = hitbox.x;
        int top = hitbox.y;
        int right = hitbox.x + hitbox.width - 1;
        int bottom = hitbox.y + hitbox.height - 1;

        return isSolidAtPixel(left, top)
                || isSolidAtPixel(right, top)
                || isSolidAtPixel(left, bottom)
                || isSolidAtPixel(right, bottom);
    }

    public int worldToColumn(int worldX) {
        return worldX / TILE_SIZE;
    }

    public int worldToRow(int worldY) {
        return worldY / TILE_SIZE;
    }

    public int getMapWidthPixels() {
        return mapGrid[0].length * TILE_SIZE;
    }

    public int getMapHeightPixels() {
        return mapGrid.length * TILE_SIZE;
    }

    public int[][] getMapGrid() {
        return mapGrid;
    }

    private void loadAndSliceTiles(String resourcePath) {
        try (InputStream stream = TileManager.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                buildFallbackTiles();
                return;
            }

            BufferedImage spritesheet = ImageIO.read(stream);
            if (spritesheet == null) {
                buildFallbackTiles();
                return;
            }

            // index 0 floor, index 1 wall, index 2 void
            tiles[TILE_FLOOR] = spritesheet.getSubimage(0, 0, TILE_SIZE, TILE_SIZE);
            tiles[TILE_WALL] = spritesheet.getSubimage(TILE_SIZE, 0, TILE_SIZE, TILE_SIZE);
            tiles[TILE_VOID] = spritesheet.getSubimage(TILE_SIZE * 2, 0, TILE_SIZE, TILE_SIZE);
        } catch (IOException ex) {
            buildFallbackTiles();
        }
    }

    private void buildFallbackTiles() {
        tiles[TILE_FLOOR] = createSolidTile(new Color(54, 71, 88), new Color(45, 60, 75));
        tiles[TILE_WALL] = createSolidTile(new Color(17, 25, 36), new Color(12, 18, 28));
        tiles[TILE_VOID] = createSolidTile(new Color(7, 14, 22), new Color(5, 10, 16));
    }

    private BufferedImage createSolidTile(Color base, Color line) {
        BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tile.createGraphics();
        g2.setColor(base);
        g2.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g2.setColor(line);
        g2.drawRect(0, 0, TILE_SIZE - 1, TILE_SIZE - 1);
        g2.drawLine(0, TILE_SIZE / 2, TILE_SIZE, TILE_SIZE / 2);
        g2.dispose();
        return tile;
    }

    private int[][] createSampleSubmarineGrid() {
        // 0 floor, 1 wall, 2 void
        return new int[][] {
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
        };
    }

    private boolean isInsideGrid(int row, int col) {
        return row >= 0 && row < mapGrid.length && col >= 0 && col < mapGrid[0].length;
    }
}
