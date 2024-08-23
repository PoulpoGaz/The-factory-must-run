package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Content;
import fr.poulpogaz.run.ContentType;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.Tile;

import static fr.poulpogaz.run.Variables.atlas;
import static fr.poulpogaz.run.Variables.batch;

public abstract class Block extends Content {

    protected TextureRegion region;

    public Block(String name) {
        super(name);
    }

    @Override
    public void load() {
        region = atlas.findRegion(name);
    }

    public void drawBuildPlan(float x, float y, Direction direction) {
        float color = batch.getPackedColor();
        batch.setColor(1, 1, 1, 0.5f);
        Utils.draw(region, x, y, canBeRotated() ? direction.angle : 0);
        batch.setPackedColor(color);
    }

    public void draw(Tile tile) {
        batch.draw(region, tile.drawX(), tile.drawY());
    }

    public void tick(BlockData data) {

    }

    public void onBlockBuild(Tile tile) {

    }

    public void onBlockRotated(Tile tile, Direction newDirection) {

    }

    public void onBlockReplaced(Tile tile, BlockData oldData) {

    }

    public void onBlockDestroyed(Tile tile) {

    }

    public BlockData createData(Tile tile) {
        return null;
    }

    public abstract int width();

    public abstract int height();

    public boolean canBeRotated() {
        return false;
    }

    // true if data is ConveyorData
    public boolean isConveyor() {
        return false;
    }

    public boolean isAir() {
        return false;
    }

    public boolean isUpdatable() {
        return false;
    }

    @Override
    public final ContentType getContentType() {
        return ContentType.BLOCK;
    }
}
