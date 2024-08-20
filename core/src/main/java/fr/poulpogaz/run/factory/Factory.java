package fr.poulpogaz.run.factory;

import com.badlogic.gdx.utils.Array;
import fr.poulpogaz.run.Rotation;
import fr.poulpogaz.run.factory.blocks.Block;
import fr.poulpogaz.run.factory.blocks.BlockData;
import fr.poulpogaz.run.factory.blocks.Blocks;

import java.util.ArrayList;
import java.util.List;

public class Factory {

    public Tile[] tiles;
    public int width;
    public int height;

    public Array<Block> blocks = new Array<>();

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
                    tiles[i].setBlock(Blocks.WALL, Rotation.DEGREES_0);
                }

                i++;
            }
        }
    }

    public void tick() {
        tick++;
    }

    public void setBlock(int x, int y, Block selectedBlock, Rotation rotation) {
        getTile(x, y).setBlock(selectedBlock, rotation);
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
