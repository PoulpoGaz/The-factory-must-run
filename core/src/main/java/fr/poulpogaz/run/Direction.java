package fr.poulpogaz.run;

import com.badlogic.gdx.math.Vector2;

public enum Direction {

    LEFT(0, 1, 0),
    UP(90, 0, 1),
    RIGHT(180, -1, 0),
    DOWN(270, 0, -1);


    static {
        LEFT.next = UP;
        UP.next = RIGHT;
        RIGHT.next = DOWN;
        DOWN.next = LEFT;

        LEFT.previous = DOWN;
        UP.previous = LEFT;
        RIGHT.previous = UP;
        DOWN.previous = RIGHT;

        LEFT.opposite = RIGHT;
        UP.opposite = DOWN;
        RIGHT.opposite = LEFT;
        DOWN.opposite = UP;
    }

    public static final Direction[] values = Direction.values();

    public final float angle;
    public final int dx;
    public final int dy;

    private Direction next;
    private Direction previous;
    private Direction opposite;

    Direction(float angle, int dx, int dy) {
        this.angle = angle;
        this.dx = dx;
        this.dy = dy;
    }

    public Direction rotate() {
        return next;
    }

    public Direction rotateCW() {
        return previous;
    }

    public Direction opposite() {
        return opposite;
    }

    public void rotate(Vector2 v) {
        v.set(
            v.x * dx - v.y * dy,
            v.x * dy + v.y * dx
        );
    }

    public RelativeDirection relativeTo(Direction reference) {
        return RelativeDirection.relative(reference, this);
    }
}
