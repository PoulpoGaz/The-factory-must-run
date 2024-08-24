package fr.poulpogaz.run;

import com.badlogic.gdx.math.Vector2;

public enum Direction {

    RIGHT(0, 1, 0),
    UP(90, 0, 1),
    LEFT(180, -1, 0),
    DOWN(270, 0, -1);


    static {
        LEFT.previous = UP;
        UP.previous = RIGHT;
        RIGHT.previous = DOWN;
        DOWN.previous = LEFT;

        LEFT.next = DOWN;
        UP.next = LEFT;
        RIGHT.next = UP;
        DOWN.next = RIGHT;

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
