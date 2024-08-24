package fr.poulpogaz.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import fr.poulpogaz.run.factory.blocks.Block;
import fr.poulpogaz.run.factory.blocks.Blocks;

import static fr.poulpogaz.run.Variables.*;

public class GameInput extends BasicInputProcessor {

    public final Vector3 world = new Vector3();

    public int tileX;
    public int tileY;

    public Block selectedBlock;
    public Direction selectedBlockDirection = Direction.RIGHT;
    public boolean flipped = false;

    public boolean pause = false;
    public int simulateTicks = 0;

    public GameInput() {

    }

    public void update() {
        world.set(mouseX, mouseY, 0);
        camera.unproject(world);

        tileX = (int) (world.x / TILE_SIZE);
        tileY = (int) (world.y / TILE_SIZE);

        moveCamera();

        if (factory.isInFactory(tileX, tileY) && isMousePressed(Input.Buttons.RIGHT)) {
            factory.setBlock(tileX, tileY, Blocks.AIR);
        }

        if (selectedBlock != null) {
            if (factory.isInFactory(tileX, tileY) && isMousePressed(Input.Buttons.LEFT)) {
                factory.setBlock(tileX, tileY, selectedBlock, selectedBlockDirection, flipped);
            }
            if (factory.isInFactory(tileX, tileY) && isMousePressed(Input.Buttons.RIGHT)) {
                factory.removeBlock(tileX, tileY);
            }

            boolean shift = isKeyPressed(Input.Keys.SHIFT_LEFT);
            boolean r = isKeyJustPressed(Input.Keys.R);

            if (selectedBlock.canBeRotated() && !shift && r) {
                selectedBlockDirection = selectedBlockDirection.rotateCW();
            }
            if (selectedBlock.canBeFlipped() && shift && r) {
                flipped = !flipped;
            }

            if (isKeyJustPressed(Input.Keys.Q)) {
                select(null);
            }
        }

        if (isKeyJustPressed(Input.Keys.SPACE)) {
            pause = !pause;
        }

        if (isKeyJustPressed(Input.Keys.LEFT)) {
            simulateTicks = 1;
        }
    }

    public void select(Block block) {
        selectedBlock = block;

        if (block != null && !block.canBeRotated()) {
            selectedBlockDirection = Direction.LEFT;
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
