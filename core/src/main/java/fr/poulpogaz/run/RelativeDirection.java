package fr.poulpogaz.run;

import fr.poulpogaz.run.factory.blocks.Block;
import fr.poulpogaz.run.factory.blocks.BlockData;

public enum RelativeDirection implements ToAbsolute {

    // DO NOT MOVE ORDER
    FACING {
        @Override
        public Direction absolute(Direction reference) {
            return reference;
        }
    },
    LEFT {
        @Override
        public Direction absolute(Direction reference) {
            return reference.rotate();
        }
    },
    RIGHT {
        @Override
        public Direction absolute(Direction reference) {
            return reference.rotateCW();
        }
    },
    BEHIND {
        @Override
        public Direction absolute(Direction reference) {
            return reference.opposite();
        }
    };

    static {
        FACING.left = LEFT;
        LEFT.left = BEHIND;
        BEHIND.left = RIGHT;
        RIGHT.left = FACING;

        FACING.right = RIGHT;
        LEFT.right = FACING;
        BEHIND.right = LEFT;
        RIGHT.right = BEHIND;

        FACING.opposite = BEHIND;
        LEFT.opposite = RIGHT;
        BEHIND.opposite = FACING;
        RIGHT.opposite = LEFT;
    }

    public static final RelativeDirection[] values = RelativeDirection.values();

    private RelativeDirection left;
    private RelativeDirection right;
    private RelativeDirection opposite;

    public RelativeDirection left() {
        return left;
    }

    public RelativeDirection right() {
        return right;
    }

    public RelativeDirection behind() {
        return opposite;
    }

    public static RelativeDirection relative(Direction reference, Direction direction) {
        if (reference == direction) {
            return RelativeDirection.FACING;
        } else if (reference.rotate() == direction) {
            return RelativeDirection.LEFT;
        } else if (reference.opposite() == direction) {
            return RelativeDirection.BEHIND;
        } else {
            return RelativeDirection.RIGHT;
        }
    }

    /**
     * @return the direction to take, relative to block direction, to find other
     */
    public static RelativeDirection relativePos(BlockData block, BlockData other) {
        if (block.adjacent(block.direction) == other.tile) {
            return RelativeDirection.FACING;
        } else if (block.adjacent(block.direction.rotate()) == other.tile) {
            return RelativeDirection.LEFT;
        } else if (block.adjacent(block.direction.opposite()) == other.tile) {
            return RelativeDirection.BEHIND;
        } else {
            return RelativeDirection.RIGHT;
        }
    }
}
