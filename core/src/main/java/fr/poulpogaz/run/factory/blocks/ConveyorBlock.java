package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Rotation;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.Tile;

import static fr.poulpogaz.run.Variables.*;

public class ConveyorBlock extends Block {

    private TextureRegion[] straight;
    private TextureRegion[] turn;
    private TextureRegion[] turnFlipped;

    public ConveyorBlock(String name) {
        super(name);
        canBeRotated = true;
    }

    @Override
    public void load() {
        super.load();

        straight = loadRegions("conveyor");
        turn = loadRegions("conveyor_turn");

        turnFlipped = new TextureRegion[turn.length];
        for (int i = 0; i < turn.length; i++) {
            turnFlipped[i] = Utils.flip(turn[i], false, true);
        }
    }

    private TextureRegion[] loadRegions(String name) {
        TextureRegion[] r = new TextureRegion[8];
        for (int i = 0; i < r.length; i++) {
            r[i] = atlas.findRegion(name, i + 1);
        }

        return r;
    }

    @Override
    public void draw(Tile tile) {
        Data data = (Data) tile.getBlockData();

        boolean left = canConnect(tile, data.rotation.rotate());
        boolean behind = canConnect(tile, data.rotation.opposite());
        boolean right = canConnect(tile, data.rotation.rotateCW());

        int frame = (factory.getTick() / 8) % 8;

        TextureRegion[] textures;
        if (left && !behind && !right) {
            textures = turnFlipped;
        } else if (right && !behind && !left) {
            textures = turn;
        } else {
            textures = straight;
        }

        Utils.draw(textures[frame], data.drawX(), data.drawY(), data.rotation.angle);
    }

    private boolean canConnect(Tile tile, Rotation vec) {
        Tile side = tile.adjacent(vec);
        BlockData b = side.getBlockData();

        if (b == null) {
            return false;
        }

        return side.getBlock() instanceof ConveyorBlock && b.rotation == vec.opposite();
    }

    @Override
    public void tick(BlockData data) {
        Data d = (Data) data;
    }

    @Override
    public BlockData createData() {
        return new Data();
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public int height() {
        return 1;
    }

    public static class Data extends BlockData {


    }
}
