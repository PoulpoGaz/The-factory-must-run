package fr.poulpogaz.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Factory;
import fr.poulpogaz.run.factory.Floor;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.blocks.*;

import static fr.poulpogaz.run.Variables.*;

public class GameScreen implements Screen {

    private static final Matrix4 ID = new Matrix4();

    private final Stage stage = new Stage();
    private final Skin skin;
    private Table rootTable;
    private ButtonGroup<Button> group = new ButtonGroup<>();

    public GameScreen() {
        skin = new Skin(Gdx.files.internal("textures/uiskin.json"));
        stage.setViewport(new ScreenViewport());

        createUI();
    }

    private void createUI() {
        rootTable = new Table();
        rootTable.setFillParent(true);

        rootTable.bottom();
        createButton(Blocks.CONVEYOR, "conveyor");
        createButton(Blocks.ROUTER, "router");
        createButton(Blocks.UNDERGROUND_CONVEYOR, "underground_conveyor_input");
        createButton(Blocks.GENERATOR, "generator");
        createButton(Blocks.CONSUMER, "consumer");
        createButton(Blocks.MACHINE, "machine");
        createButton(Blocks.WALL, "wall");

        stage.addActor(rootTable);
    }

    private void createButton(Block block, String icon) {
        TextureRegion r = atlas.findRegion(icon);

        Table table = new Table();
        table.add(new Image(new TextureRegionDrawable(r), Scaling.fit)).padRight(5);
        table.add(new Label(Integer.toString(block.value()), skin));

        Button button = new Button(table, skin, "toggle") {
            @Override
            public float getPrefWidth() {
                return TILE_SIZE * 2.5f;
            }

            @Override
            public float getPrefHeight() {
                return (float) (TILE_SIZE * 1.25);
            }

        };
        button.addListener(createChangeLister(block));

        group.add(button);
        rootTable.add(button).padLeft(20).padRight(20);
    }

    private static ChangeListener createChangeLister(Block block) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (input != null && ((Button) actor).isChecked()) {
                    input.select(block);
                }
            }
        };
    }

    @Override
    public void show() {
        input = new GameInput();
        factory = new Factory();
        camera = new Camera();

        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
    }

    @Override
    public void render(float delta) {
        update(delta);
        ScreenUtils.clear(0, 0, 0, 1);
        renderFactory();

        setupBatches(stage.getViewport().getCamera().combined, ID);

        if (debug) {
            if (input.pause) {
                font.draw(batch, "Paused", 0, Gdx.graphics.getHeight());
            }
            font.draw(batch, "Debug", 0, Gdx.graphics.getHeight() - 20);
        }

        if (playerResources < 0) {
            font32.setColor(1, 0, 0, 1);
        } else {
            font32.setColor(1, 1, 1, 1);
        }

        if (!bankruptcyContinue) {
            Utils.drawStringCentered(font32, "Resources: " + playerResources, 0, Gdx.graphics.getHeight(),
                                     Gdx.graphics.getWidth());
            if (playerResources < 0) {
                int diff = (TIME_BEFORE_BANKRUPTCY - (factory.tick - bankruptcyTick)) / 60;

                int min = diff / 60;
                int sec = diff % 60;

                if (min > 0) {
                    Utils.drawStringCentered(font32, "Bankruptcy in : " + min + "m " + sec + "s", 0,
                                             Gdx.graphics.getHeight() - 30, Gdx.graphics.getWidth());
                } else {
                    Utils.drawStringCentered(font32, "Bankruptcy in : " + min + "m", 0, Gdx.graphics.getHeight() - 30,
                                             Gdx.graphics.getWidth());
                }
            }
        }

        if (hasReachedBankruptcy && !bankruptcyContinue) {
            TextureRegion white = ItemGUI.white;

            float color = batch.getPackedColor();
            batch.setColor(0.5f, 0.5f, 0.5f, 0.5f);
            batch.draw(white, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.setPackedColor(color);

            Utils.drawStringCentered(font32, "Your factory goes bankrupt.\nHowever, you can swap to sandbox modes by pressing SPACE.", 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } else if (input.pause && !debug) {
            TextureRegion white = ItemGUI.white;

            float color = batch.getPackedColor();
            batch.setColor(0.5f, 0.5f, 0.5f, 0.5f);
            batch.draw(white, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.setPackedColor(color);

            Utils.drawStringCentered(font32, "Game paused.\nSPACE to resume.", 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

        batch.end();

        stage.draw();
    }


    private void update(float delta) {
        input.update();

        if (input.selectedBlock == null && group.getChecked() != null) {
            group.uncheckAll();
        }


        if (!input.pause && (!hasReachedBankruptcy || bankruptcyContinue) || input.simulateTicks > 0) {
            factory.tick();

            input.simulateTicks = Math.max(input.simulateTicks - 1, 0);

            if (playerResources >= 0) {
                bankruptcyTick = factory.tick;
            }

            if (!bankruptcyContinue && factory.tick - bankruptcyTick >= TIME_BEFORE_BANKRUPTCY) {
                hasReachedBankruptcy = true;
                input.pause = true;
            }
        }
        stage.act(Math.min(delta, 1 / 60f));
        input.clean();
    }

    private void renderFactory() {
        setupBatches(camera.combined, ID);

        batch.begin();

        drawFactory();

        if (debug) {
            ConveyorManager.drawGraphs();
        }

        // draw build plan
        if (input.gui != null) {
            input.gui.drawGUI(input.guiTile, input.guiRectangle);
        }

        if (input.gui == null || !input.guiRectangle.contains(input.world.x, input.world.y)) {
            if (input.selectedBlock != null) {
                input.selectedBlock.drawBuildPlan(input.tileX * TILE_SIZE,
                                                  input.tileY * TILE_SIZE,
                                                  input.selectedBlockDirection,
                                                  input.flipped);

                if (input.selectedBlock == Blocks.UNDERGROUND_CONVEYOR) {
                    if (input.selectedUndergroundConveyor != null) {
                        Tile t = input.selectedUndergroundConveyor;
                        drawUndergroundConveyorRange((UndergroundConveyorBlock) t.getBlock(),
                                                     t.x, t.y);
                    } else {
                        drawUndergroundConveyorRange((UndergroundConveyorBlock) input.selectedBlock,
                                                     input.tileX, input.tileY);
                    }
                }
            } else if (input.selectedUndergroundConveyor != null) {
                Tile t = input.selectedUndergroundConveyor;
                drawUndergroundConveyorRange((UndergroundConveyorBlock) t.getBlock(), t.x, t.y);
            }
        }
    }


    private void drawFactory() {
        for (Tile tile : factory.tiles) {
            Floor floor = tile.getFloor();
            TextureRegion region = floor.getRegion();
            batch.draw(region, tile.drawX(), tile.drawY());
        }

        for (int y = 0; y < factory.height; y++) {
            for (int x = 0; x < factory.width; x++) {
                Tile tile = factory.getTile(x, y);

                if (tile.multiBlockOffsetX > 0 || tile.multiBlockOffsetY > 0) {
                    continue;
                }

                Block block = tile.getBlock();
                if (block != null) {
                    block.draw(tile);
                }
            }
        }

        ConveyorManager.drawItems();
        // draw router filter because no depth is set up
        for (Tile tile : factory.tiles) {
            if (tile.getBlock() instanceof RouterBlock) {
                RouterBlock.Data data = (RouterBlock.Data) tile.getBlockData();

                if (data.getFilter() != null) {
                    batch.draw(data.getFilter().getIcon(),
                               tile.drawX() + HALF_TILE_SIZE / 2f + data.direction.dx * HALF_TILE_SIZE / 2f,
                               tile.drawY() + HALF_TILE_SIZE / 2f + data.direction.dy * HALF_TILE_SIZE / 2f);
                }
            }
        }
    }

    private void drawUndergroundConveyorRange(UndergroundConveyorBlock block, int tileX, int tileY) {
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(1, 154 / 255f, 53 / 255f, 0.25f);

        int x = tileX;
        int y = tileY;

        int drawX = HALF_TILE_SIZE;
        int drawY = 0;

        int ox = x * TILE_SIZE + HALF_TILE_SIZE;
        int oy = y * TILE_SIZE + HALF_TILE_SIZE;

        while (block.isInRange(tileX, tileY, x + 1, y)) {
            x++;
            drawX += TILE_SIZE;
        }

        int len = HALF_TILE_SIZE;
        do {
            if (!block.isInRange(tileX, tileY, x, y + 1)) {
                int y1 = drawY - 1;
                int y2 = drawY + 1; // take into account line width

                // rotation
                shape.rectLine(ox + drawX, oy + y1, ox + drawX, oy + y2 + len, 2f);
                shape.rectLine(ox - y1, oy + drawX, ox - y2 - len, oy + drawX, 2f);
                shape.rectLine(ox - drawX, oy - y1, ox - drawX, oy - y2 - len, 2f);
                shape.rectLine(ox + y1, oy - drawX, ox + y2 + len, oy - drawX, 2f);

                // flip
                shape.rectLine(ox + y1, oy + drawX, ox + y2 + len, oy + drawX, 2f);
                shape.rectLine(ox + drawX, oy - y1, ox + drawX, oy - y2 - len, 2f);
                shape.rectLine(ox - y1, oy - drawX, ox - y2 - len, oy - drawX, 2f);
                shape.rectLine(ox - drawX, oy + y1, ox - drawX, oy + y2 + len, 2f);
                x--;
                drawX -= TILE_SIZE;
                drawY = drawY + len;
                len = 0;
            } else {
                len += TILE_SIZE;
                y++;
            }
        } while (x >= tileX);

        shape.end();
        batch.begin();
    }

    private void setupBatches(Matrix4 proj, Matrix4 transform) {
        batch.setProjectionMatrix(proj);
        shape.setProjectionMatrix(proj);
        batch.setTransformMatrix(transform);
        shape.setTransformMatrix(transform);
    }

    @Override
    public void resize(int width, int height) {
        camera.resize(width, height);
        camera.update();
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        input = null;
        camera = null;
        factory = null;


        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {

    }
}
