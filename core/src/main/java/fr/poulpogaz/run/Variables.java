package fr.poulpogaz.run;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import fr.poulpogaz.run.factory.Factory;

public class Variables {

    public static final int TILE_SIZE = 32;
    public static final int HALF_TILE_SIZE = TILE_SIZE / 2;
    public static final int DEFAULT_RESOURCES = 1000;

    public static final AssetManager assetManager = new AssetManager();
    public static ShapeRenderer shape;
    public static SpriteBatch batch;
    public static BitmapFont font;
    public static TextureAtlas atlas;

    public static GameInput input;
    public static Camera camera;
    public static Factory factory;
    public static int playerResources = DEFAULT_RESOURCES;

    public static boolean debug = true;
}
