package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.Tile;

public class BlockData {

    // tile on which the block is.
    // if multi block -> top left tile
    public Tile tile;

    public Direction direction = Direction.LEFT;

    public int drawX() {
        return tile.drawX();
    }

    public int drawY() {
        return tile.drawY();
    }
}
