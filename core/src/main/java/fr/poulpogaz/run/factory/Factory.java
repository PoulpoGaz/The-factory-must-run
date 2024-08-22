package fr.poulpogaz.run.factory;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.blocks.Block;
import fr.poulpogaz.run.factory.blocks.BlockData;
import fr.poulpogaz.run.factory.blocks.Blocks;

public class Factory {

    public Tile[] tiles;
    public int width;
    public int height;


    public int tick = 0;

    public Factory() {
        this(20, 20);
    }

    public Factory(int width, int height) {
        this.tiles = new Tile[width * height];
        this.width = width;
        this.height = height;

        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[i] = new Tile(x, y);

                if (y == 0 || y == height - 1 || x == 0 || x == width - 1) {
                    tiles[i].setBlock(Blocks.WALL, Direction.LEFT);
                }

                i++;
            }
        }
    }

    public void tick() {
        tick++;

        for (int i = 0; i < tiles.length; i++) {
            Tile tile = tiles[i];

            if (tile.getBlock().isUpdatable()) {
                tile.getBlock().tick(tile.getBlockData());
            }
        }

        ConveyorManager.tick();
    }

    public void setBlock(int x, int y, Block selectedBlock) {
        setBlock(x, y, selectedBlock, Direction.LEFT);
    }

    public void setBlock(int x, int y, Block selectedBlock, Direction direction) {
        if (direction == null) {
            direction = Direction.LEFT;
        }
        getTile(x, y).setBlock(selectedBlock, direction);
    }

    public Tile getTile(int x, int y) {
        if (isInFactory(x, y)) {
            return tiles[y * width + x];
        } else {
            return null;
        }
    }

    public Block getBlock(int x, int y) {
        Tile t = getTile(x, y);

        if (t == null) {
            return null;
        }

        return t.getBlock();
    }

    public BlockData getBlockData(int x, int y) {
        Tile t = getTile(x, y);

        if (t == null) {
            return null;
        }

        return t.getBlockData();
    }

    public boolean isInFactory(int x, int y) {
        return 0 <= x && x < width && 0 <= y && y < height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTick() {
        return tick;
    }
}
