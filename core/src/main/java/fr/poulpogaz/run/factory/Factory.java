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
                    tiles[i].setBlock(Blocks.WALL, Direction.LEFT, false, 0, 0);
                }

                i++;
            }
        }
    }

    public void tick() {
        tick++;

        for (Tile tile : tiles) {
            if (tile.getBlock().isUpdatable() && tile.isMultiBlockAnchor()) {
                tile.getBlock().tick(tile.getBlockData());
            }
        }

        ConveyorManager.tick();
    }

    public boolean setBlock(int x, int y, Block selectedBlock) {
        return setBlock(x, y, selectedBlock, Direction.LEFT, false);
    }

    public boolean setBlock(int x, int y, Block selectedBlock, Direction direction, boolean flipped) {
        if (x < 0 || y < 0 || x + selectedBlock.width() > width || y + selectedBlock.height() > height) {
            return false;
        }

        if (direction == null) {
            direction = Direction.LEFT;
        }

        if (selectedBlock.isMultiBlock()) {
            Tile tile = getTile(x, y);
            if (tile.isMultiBlockAnchor() && tile.block == selectedBlock) {
                // rotate / flip
                getTile(x, y).setBlock(selectedBlock,direction, flipped, 0, 0);
            } else if (tile.block != selectedBlock) {
                // check if same block in area
                for (int offsetY = 0; offsetY < selectedBlock.height(); offsetY++) {
                    for (int offsetX = 0; offsetX < selectedBlock.width(); offsetX++) {
                        if (getBlock(x + offsetX, y + offsetY) == selectedBlock) {
                            return false;
                        }
                    }
                }

                for (int offsetY = 0; offsetY < selectedBlock.height(); offsetY++) {
                    for (int offsetX = 0; offsetX < selectedBlock.width(); offsetX++) {
                        removeBlock(x + offsetX, y + offsetY);
                        getTile(x + offsetX, y + offsetY).setBlock(selectedBlock, direction, flipped, offsetX, offsetY);
                    }
                }
            }

            return true;
        } else {
            Tile tile = getTile(x, y);
            Block block = tile.getBlock();

            if (block.isMultiBlock()) {
                Tile anchor = tile.multiBlockAnchor();

                for (int offsetY = 0; offsetY < block.height(); offsetY++) {
                    for (int offsetX = 0; offsetX < block.width(); offsetX++) {
                        Tile t = getTile(anchor.x + offsetX, anchor.y + offsetY);
                        t.setBlock(Blocks.AIR, null, false, 0, 0);
                    }
                }
            }

            return tile.setBlock(selectedBlock, direction, flipped, 0, 0);
        }
    }

    public void removeBlock(int x, int y) {
        setBlock(x, y, Blocks.AIR, null, false);
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
