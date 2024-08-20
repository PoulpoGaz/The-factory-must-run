package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Rotation;
import fr.poulpogaz.run.factory.Tile;

public class BlockData {

    // tile on which the block is.
    // if multi block -> top left tile
    public Tile tile;

    public Rotation rotation = Rotation.DEGREES_0;

    public int drawX() {
        return tile.drawX();
    }

    public int drawY() {
        return tile.drawY();
    }
}
