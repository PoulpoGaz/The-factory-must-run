package fr.poulpogaz.run.factory;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.blocks.Block;
import fr.poulpogaz.run.factory.blocks.BlockData;
import fr.poulpogaz.run.factory.blocks.Blocks;
import fr.poulpogaz.run.factory.blocks.IFlipData;

import static fr.poulpogaz.run.Variables.TILE_SIZE;
import static fr.poulpogaz.run.Variables.factory;

public class Tile {

    private Block block = Blocks.AIR;
    private BlockData data = null;
    private Floor floor = Floor.GROUND;

    public final int x;
    public final int y;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }


    public Tile adjacent(Direction direction) {
        return adjacent(direction.dx, direction.dy);
    }

    public Tile adjacent(int dx, int dy) {
        return factory.getTile(x + dx, y + dy);
    }

    public void setBlock(Block block, Direction direction, boolean flipped) {
        if (block == Blocks.AIR || block == null) {
            if (this.block != Blocks.AIR) {
                // remove
                this.block.onBlockDestroyed(this);
                if (data != null) {
                    data.tile = null;
                }
                data = null;
                this.block = Blocks.AIR;
            }
        } else if (this.block == block) {
            // rotate
            if (data == null) {
                return;
            }
            if (data.direction == direction) {
                return; // do not replace if same block with same rotation
            }

            block.onBlockRotated(this, direction);
            data.direction = direction;
        } else  {
            // replace
            Block oldBlock = this.block;
            BlockData oldData = this.data;
            oldBlock.onBlockDestroyed(this);

            this.block = block;
            data = block.createData(this);

            if (data != null) {
                data.tile = this;
                data.direction = direction;

                if (data instanceof IFlipData) {
                    ((IFlipData) data).setFlipped(flipped);
                }
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
