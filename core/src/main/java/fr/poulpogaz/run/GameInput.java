package fr.poulpogaz.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import fr.poulpogaz.run.factory.blocks.Block;

import static fr.poulpogaz.run.Variables.*;

public class GameInput extends BasicInputProcessor {

    public final Vector3 world = new Vector3();

    public int tileX;
    public int tileY;

    public Block selectedBlock;
    public Rotation selectedBlockRotation = Rotation.DEGREES_0;

    public GameInput() {

    }

    public void update() {
        world.set(mouseX, mouseY, 0);
        camera.unproject(world);

        tileX = (int) (world.x / TILE_SIZE);
        tileY = (int) (world.y / TILE_SIZE);

        moveCamera();

        if (selectedBlock != null) {
            if (factory.isInFactory(tileX, tileY) && isMousePressed(Input.Buttons.LEFT)) {
                factory.setBlock(tileX, tileY, selectedBlock, selectedBlockRotation);
            }

            if (selectedBlock.canBeRotated() && isKeyJustPressed(Input.Keys.R)) {
                selectedBlockRotation = selectedBlockRotation.rotateCW();
            }

            if (isKeyJustPressed(Input.Keys.A)) {
                select(null);
            }
        }
    }

    public void select(Block block) {
        selectedBlock = block;

        if (block != null && !block.canBeRotated()) {
            selectedBlockRotation = Rotation.DEGREES_0;
        }
    }

    private void moveCamera() {
        if (dragged) {
            camera.translate(camera.zoom * dragX * camera.viewportWidth / Gdx.graphics.getWidth(),
                             camera.zoom * dragY * camera.viewportWidth / Gdx.graphics.getWidth());
        }

        if (scroll != 0) {
            camera.zoom(scroll,
                        mouseX * camera.viewportWidth / Gdx.graphics.getWidth(),
                        mouseY * camera.viewportHeight / Gdx.graphics.getHeight());
        }

        camera.update();
    }
}
