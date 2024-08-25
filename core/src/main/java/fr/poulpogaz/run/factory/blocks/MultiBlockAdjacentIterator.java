package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.Tile;

public class MultiBlockAdjacentIterator {

    public static final MultiBlockAdjacentIterator instance = new MultiBlockAdjacentIterator();

    private Tile tile;
    private Block block;

    public Direction normal;


    private Tile adj;
    private Direction dir;
    private int x;
    private int y;

    public MultiBlockAdjacentIterator() {

    }

    public void set(Tile tile) {
        this.tile = tile.multiBlockAnchor();
        this.block = tile.block;

        x = block.width();
        y = -1;
        dir = Direction.UP;
        normal = Direction.RIGHT;
        adj = null;
    }

    public Tile next() {
        if (!hasNext()) {
            return null;
        }

        Tile ret = adj;
        adj = null;
        return ret;
    }

    private void computeNext() {
        if (x == block.width() - 1 && y == -1) {
            return;
        }

        do {
            x += dir.dx;
            y += dir.dy;

            if (x == block.width() && (y == -1 || y == block.height())
                || x == -1 && (y == -1 || y == block.height())) {
                dir = dir.rotate();
                normal = dir.rotateCW();
                x += dir.dx;
                y += dir.dy;
            }

        } while (tile.adjacent(x, y) == null && (x != block.width() - 1 || y != -1));

        adj = tile.adjacent(x, y);
    }

    public boolean hasNext() {
        if (adj != null) {
            return true;
        } else {
            computeNext();

            return adj != null || x != block.width() - 1 || y != -1;
        }
    }
}
