package fr.poulpogaz.run;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import static fr.poulpogaz.run.Variables.*;

public class Main extends Game {

    public static final int WIDTH = 1080;
    public static final int HEIGHT = WIDTH * 9 / 16;

    private int oldWidth = WIDTH;
    private int oldHeight = HEIGHT;

    @Override
    public void create() {
        // load assets
        assetManager.load("textures/font.fnt", BitmapFont.class);
        assetManager.load("textures/font_32.fnt", BitmapFont.class);
        assetManager.load("textures/map/map.atlas", TextureAtlas.class);

        assetManager.finishLoading();

        // setup variables
        shape = new ShapeRenderer();
        batch = new SpriteBatch();

        font = assetManager.get("textures/font.fnt");
        font32 = assetManager.get("textures/font_32.fnt");
        atlas = assetManager.get("textures/map/map.atlas");

        // load content
        ContentManager.createContent();
        ContentManager.loadContent();

        setScreen(new GameScreen());
    }

    @Override
    public void render() {
        super.render();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)){
            debug = !debug;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            boolean fullScreen = Gdx.graphics.isFullscreen();
            Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();

            if (fullScreen) {
                Gdx.graphics.setWindowedMode(oldWidth, oldHeight);
            } else {
                oldWidth = Gdx.graphics.getWidth();
                oldHeight = Gdx.graphics.getHeight();
                Gdx.graphics.setFullscreenMode(currentMode);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        assetManager.dispose();
        shape.dispose();
        batch.dispose();
    }
}
