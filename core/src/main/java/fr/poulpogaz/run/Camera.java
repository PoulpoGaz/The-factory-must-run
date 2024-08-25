package fr.poulpogaz.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

/**
 * Higher zoom means more things can be seen.
 */
public class Camera extends OrthographicCamera {

    public float velocity = 20f;
    private float zoomT = 0;

    public Camera() {
        zoom = zoom(zoomT);
    }

    public Camera(float viewportWidth, float viewportHeight) {
        super(viewportWidth, viewportHeight);
        zoom = zoom(zoomT);
    }

    public void move(float x, float y) {
        float advance = getAdvance();
        translate(advance * x, advance * y);
    }

    public void zoom(float scroll, float mouseX, float mouseY) {
        zoomT = MathUtils.clamp(zoomT + 0.5f * scroll, -2, 3);

        float oldZoom = zoom;
        zoom = zoom(zoomT);

        float y = viewportHeight - mouseY;
        float screenX = (2 * mouseX) / viewportWidth - 1;
        float screenY = (2 * y) / viewportHeight - 1;

        float deltaX = (oldZoom - zoom) * screenX * viewportWidth / 2;
        float deltaY = (oldZoom - zoom) * screenY * viewportHeight / 2;

        position.add(deltaX, deltaY, 0);
    }

    private float zoom(float t) {
        return (float) Math.pow(2, t);
    }

    public void resize(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    private float getAdvance() {
        return velocity * zoom * Gdx.graphics.getDeltaTime();
    }
}
