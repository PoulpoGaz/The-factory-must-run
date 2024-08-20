package fr.poulpogaz.run;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Factory;
import fr.poulpogaz.run.factory.Floor;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.blocks.Block;
import fr.poulpogaz.run.factory.blocks.Blocks;

import static fr.poulpogaz.run.Variables.*;

public class GameScreen implements Screen {

    private static final Matrix4 ID = new Matrix4();

    private final Stage stage = new Stage();
    private final Skin skin;
    private Table rootTable;

    public GameScreen() {
        skin = new Skin(Gdx.files.internal("textures/uiskin.json"));
        stage.setViewport(new ScreenViewport());

        createUI();
    }

    private void createUI() {
        rootTable = new Table();
        rootTable.setFillParent(true);

        Button conveyor = new Button(new Image(atlas.findRegion("conveyor")), skin);
        conveyor.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                input.select(Blocks.CONVEYOR);
            }
        });

        Button router = new Button(new Image(atlas.findRegion("router")), skin);
        Button undergroundConveyor = new Button(new Image(atlas.findRegion("underground_conveyor_input")), skin);

        Button wall = new Button(new Image(atlas.findRegion("wall")), skin);
        wall.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                input.select(Blocks.WALL);
            }
        });

        Button generator = new Button(new Image(atlas.findRegion("generator")), skin);
        generator.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                input.select(Blocks.GENERATOR);
            }
        });


        rootTable.bottom();
        rootTable.add(conveyor).padLeft(10).padRight(10);
        rootTable.add(router).padLeft(10).padRight(10);
        rootTable.add(undergroundConveyor).padLeft(10).padRight(10);
        rootTable.add(wall).padLeft(10).padRight(10);
        rootTable.add(generator).padLeft(10).padRight(10);

        stage.addActor(rootTable);
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

        if (input.pause) {
            setupBatches(stage.getViewport().getCamera().combined, ID);
            batch.begin();
            font.draw(batch, "Paused", 0, Gdx.graphics.getHeight());
            batch.end();
        }
        stage.draw();
    }


    private void update(float delta) {
        input.update();
        if (!input.pause || input.simulateTicks > 0) {
            factory.tick();

            input.simulateTicks = Math.max(input.simulateTicks - 1, 0);
        }
        stage.act(Math.min(delta, 1 / 60f));
        input.clean();
    }

    private void renderFactory() {
        setupBatches(camera.combined, ID);

        batch.begin();

        drawFactory();
        ConveyorManager.drawItems();
        ConveyorManager.drawConveyorSections();

        // draw build plan
        if (input.selectedBlock != null) {
            input.selectedBlock.drawBuildPlan(input.tileX * TILE_SIZE,
                                              input.tileY * TILE_SIZE,
                                              input.selectedBlockRotation);
        }

        batch.end();
    }


    private void drawFactory() {
        for (int y = 0; y < factory.height; y++) {
            for (int x = 0; x < factory.width; x++) {
                Tile tile = factory.getTile(x, y);

                Floor floor = tile.getFloor();
                TextureRegion region = floor.getRegion();
                batch.draw(region, x * TILE_SIZE, y * TILE_SIZE);

                Block block = tile.getBlock();
                if (block != null) {
                    block.draw(tile);
                }
            }
        }
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
