package fr.poulpogaz.run;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import static fr.poulpogaz.run.Variables.assetManager;

public class Animation {

    private final Frame[] frames;
    private final String texturePath;
    private Texture texture;

    private int currentFrame;
    private float delta;
    private boolean paused = false;
    private boolean loop = true;

    private boolean flipX = false;
    private boolean flipY = false;

    public Animation(String file) {
        this(file, true);
    }

    public Animation(String file, boolean loop) {
        this.loop = loop;

        JsonReader jr = new JsonReader();
        JsonValue value = jr.parse(Gdx.files.getFileHandle(file, Files.FileType.Internal));

        JsonValue meta = value.get("meta");
        texturePath = meta.getString("image");
        assetManager.load(texturePath, Texture.class);

        JsonValue frames = value.get("frames");
        this.frames = new Frame[frames.size];

        int i = 0;
        for (JsonValue entry = frames.child; entry != null; entry = entry.next, i++) {
            JsonValue frame = entry.get("frame");
            this.frames[i] = new Frame(frame.getInt("x"),
                                       frame.getInt("y"),
                                       frame.getInt("w"),
                                       frame.getInt("h"),
                                       entry.getInt("duration"));
        }
    }

    public void start() {
        paused = false;
        currentFrame = 0;
        delta = 0;
    }

    public void reset() {
        paused = true;
        currentFrame = 0;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
        delta = 0;
    }

    public void render(SpriteBatch batch, float x, float y, float width, float height, float delta) {
        if (texture == null) {
            texture = assetManager.get(texturePath);
        }

        this.delta += delta;

        if (!paused && 1000 * this.delta >= frames[currentFrame].duration) {
            currentFrame++;
            this.delta = 0;

            if (currentFrame >= frames.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    currentFrame--;
                    paused = true;
                }
            }
        }

        TextureRegion r = getTextureRegion(frames[currentFrame]);
        r.flip(flipX != r.isFlipX(), flipY != r.isFlipY());
        batch.draw(r, x, y, width, height);
    }

    private TextureRegion getTextureRegion(Frame frame) {
        if (frame.r == null) {
            frame.r = new TextureRegion(texture, frame.x, frame.y, frame.w, frame.h);
        }

        return frame.r;
    }

    public void flip(boolean x, boolean y) {
        this.flipX = x;
        this.flipY = y;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    private static class Frame {

        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final int duration;

        private TextureRegion r;

        public Frame(int x, int y, int w, int h, int duration) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.duration = duration;
        }

        @Override
        public String toString() {
            return "Frame{" +
                "x=" + x +
                ", y=" + y +
                ", w=" + w +
                ", h=" + h +
                ", duration=" + duration +
                '}';
        }
    }
}
