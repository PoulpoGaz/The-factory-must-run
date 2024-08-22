package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.factory.Tile;

public class BlockData {

    // tile on which the block is.
    // if multi block -> top left tile
    public Tile tile;

    public Direction direction = Direction.LEFT;

    public Tile adjacent(Direction direction) {
        return tile.adjacent(direction);
    }

    public Tile adjacentRelative(RelativeDirection relative) {
        return tile.adjacent(relative.absolute(this.direction));
    }

    public Tile adjacent(int x, int y) {
        return tile.adjacent(x, y);
    }

    public Tile facing() {
        return adjacent(direction);
    }

    public int drawX() {
        return tile.drawX();
    }

    public int drawY() {
        return tile.drawY();
    }
}
