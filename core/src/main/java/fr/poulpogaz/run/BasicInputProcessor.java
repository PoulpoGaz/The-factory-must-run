package fr.poulpogaz.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.IntIntMap;

import java.util.Arrays;

public class BasicInputProcessor implements InputProcessor {

    protected final IntIntMap keys = new IntIntMap();
    protected final IntIntMap keysReleased = new IntIntMap();


    protected float scroll;
    protected int mouseX;
    protected int mouseY;
    protected int[] buttonPressed = new int[5];
    protected boolean[] buttonReleased = new boolean[5];

    protected boolean dragged;
    protected int dragX;
    protected int dragY;

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        this.mouseX = screenX;
        this.mouseY = screenY;

        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        buttonPressed[button]++;

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        buttonPressed[button] = 0;
        buttonReleased[button] = true;

        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (isMousePressed(Input.Buttons.MIDDLE)) { // don't known why...
            dragX = mouseX - screenX;
            dragY = screenY - mouseY; // input y-axis is inverted
            dragged = true;
        }

        mouseX = screenX;
        mouseY = screenY;

        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        keys.put(keycode, 1);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        keys.remove(keycode, 0);
        keysReleased.put(keycode, 1);
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        scroll = amountY;

        return true;
    }

    public boolean isMousePressed(int mouse) {
        return buttonPressed[mouse] > 0;
    }

    public boolean isMouseReleased(int mouse) {
        return buttonReleased[mouse];
    }

    public boolean isKeyPressed(int key) {
        return keys.get(key, 0) > 0;
    }

    public boolean isKeyJustPressed(int key) {
        return keys.get(key, 0) == 1;
    }

    public boolean isKeyReleased(int key) {
        return keysReleased.get(key, 0) > 0;
    }

    public void update() {

    }

    public void clean() {
        scroll = 0;
        dragged = false;
        Arrays.fill(buttonReleased, false);

        for (IntIntMap.Entry entry : keys.entries()) {
            keys.put(entry.key, entry.value + 1);
        }
        keysReleased.clear();
    }
}
