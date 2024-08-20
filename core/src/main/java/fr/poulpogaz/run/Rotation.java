package fr.poulpogaz.run;

import com.badlogic.gdx.math.Vector2;

public enum Rotation {

    DEGREES_0(0, 1, 0),
    DEGREES_90(90, 0, 1),
    DEGREES_180(180, -1, 0),
    DEGREES_270(270, 0, -1);


    static {
        DEGREES_0.next = DEGREES_90;
        DEGREES_90.next = DEGREES_180;
        DEGREES_180.next = DEGREES_270;
        DEGREES_270.next = DEGREES_0;

        DEGREES_0.previous = DEGREES_270;
        DEGREES_90.previous = DEGREES_0;
        DEGREES_180.previous = DEGREES_90;
        DEGREES_270.previous = DEGREES_180;

        DEGREES_0.opposite = DEGREES_180;
        DEGREES_90.opposite = DEGREES_270;
        DEGREES_180.opposite = DEGREES_0;
        DEGREES_270.opposite = DEGREES_90;
    }

    public static final Rotation[] values = Rotation.values();

    public final float angle;
    public final int dx;
    public final int dy;

    private Rotation next;
    private Rotation previous;
    private Rotation opposite;

    Rotation(float angle, int dx, int dy) {
        this.angle = angle;
        this.dx = dx;
        this.dy = dy;
    }

    public Rotation rotate() {
        return next;
    }

    public Rotation rotateCW() {
        return previous;
    }

    public Rotation opposite() {
        return opposite;
    }

    public void rotate(Vector2 v) {
        v.set(
            v.x * dx - v.y * dy,
            v.x * dy + v.y * dx
        );
    }

    public static Rotation displacementToRotation(int dx, int dy) {
        if (dx > 0) {
            return Rotation.DEGREES_0;
        } else if (dx < 0) {
            return Rotation.DEGREES_180;
        } else if (dy > 0) {
            return Rotation.DEGREES_90;
        } else {
            return Rotation.DEGREES_270;
        }
    }
}
