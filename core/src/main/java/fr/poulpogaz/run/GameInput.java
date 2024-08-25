package fr.poulpogaz.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.blocks.*;

import static fr.poulpogaz.run.Variables.*;

public class GameInput extends BasicInputProcessor {

    public final Vector3 world = new Vector3();

    public int tileX;
    public int tileY;

    public Block selectedBlock;
    public Direction selectedBlockDirection = Direction.RIGHT;
    public boolean flipped = false;

    public Tile selectedUndergroundConveyor;

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
                boolean placed = factory.setBlock(tileX, tileY, selectedBlock, selectedBlockDirection, flipped);

                if (placed) {
                    if (selectedBlock == Blocks.UNDERGROUND_CONVEYOR) {
                        if (selectedUndergroundConveyor != null) {
                            UndergroundConveyorBlock block = (UndergroundConveyorBlock) selectedUndergroundConveyor.getBlock();

                            if (block.link(selectedUndergroundConveyor, factory.getTile(tileX, tileY))) {
                                selectedUndergroundConveyor = null;
                            }
                        } else {
                            selectedUndergroundConveyor = factory.getTile(tileX, tileY);
                        }
                    }

                    if (selectedBlock.canBeFlipped()) {
                        flipped = !flipped;
                    }
                }
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

                if (selectedBlock.canBeRotated()) {
                    selectedBlockDirection = selectedBlockDirection.opposite();
                }
            }

            if (isKeyJustPressed(Input.Keys.Q)) {
                select(null);
            }
        } else {
            if (factory.isInFactory(tileX, tileY) && isMouseJustPressed(Input.Buttons.LEFT)) {
                Tile tile = factory.getTile(tileX, tileY);

                if (tile.getBlock() == Blocks.UNDERGROUND_CONVEYOR) {
                    if (selectedUndergroundConveyor != tile && selectedUndergroundConveyor != null) {
                        UndergroundConveyorBlock block = (UndergroundConveyorBlock) selectedUndergroundConveyor.getBlock();

                        Tile tile2 = factory.getTile(tileX, tileY);

                        boolean actionDone;
                        if (block.areLinked(selectedUndergroundConveyor, tile2)) {
                            actionDone = block.unlink(selectedUndergroundConveyor);
                        } else {
                            actionDone = block.link(selectedUndergroundConveyor, factory.getTile(tileX, tileY));
                        }

                        if (actionDone) {
                            selectedUndergroundConveyor = null;
                        } else {
                            selectedUndergroundConveyor = tile2;
                        }
                    } else {
                        selectedUndergroundConveyor = tile;
                    }
                } else {
                    selectedUndergroundConveyor = null;
                }
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
