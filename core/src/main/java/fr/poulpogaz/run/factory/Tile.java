package fr.poulpogaz.run.factory;

import fr.poulpogaz.run.Rotation;
import fr.poulpogaz.run.factory.blocks.Block;
import fr.poulpogaz.run.factory.blocks.BlockData;

import static fr.poulpogaz.run.Variables.TILE_SIZE;
import static fr.poulpogaz.run.Variables.factory;

public class Tile {

    private Block block = null;
    private BlockData data = null;
    private Floor floor = Floor.GROUND;

    public final int x;
    public final int y;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }


    public Tile adjacent(Rotation rotation) {
        return adjacent(rotation.dx, rotation.dy);
    }

    public Tile adjacent(int dx, int dy) {
        return factory.getTile(x + dx, y + dy);
    }

    public void setBlock(Block block, Rotation rotation) {
        if (block == null) {
            if (this.block != null) {
                // remove
                this.block.onBlockDestroyed(this);
                data.tile = null;
                data = null;
                this.block = null;
            }
        } else if (this.block == block) {
            // replace
            if (data == null) {
                return;
            }
            if (data.rotation == rotation) {
                return; // do not replace if same block with same rotation
            }

            Rotation old = this.data.rotation;;
            data.rotation = rotation;
            block.onBlockRotated(this, old);
        } else {
            // build
            this.block = block;
            data = block.createData();

            if (data != null) {
                data.tile = this;
                data.rotation = rotation;
            }

            block.onBlockBuild(this);
        }
    }

    public Block getBlock() {
        return block;
    }

    public BlockData getBlockData() {
        return data;
    }

    public void setFloor(Floor floor) {
        this.floor = floor;
    }

    public Floor getFloor() {
        return floor;
    }

    public int drawX() {
        return x * TILE_SIZE;
    }

    public int drawY() {
        return y * TILE_SIZE;
    }
}
