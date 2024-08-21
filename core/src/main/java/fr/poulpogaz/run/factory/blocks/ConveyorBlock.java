package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.Variables.*;

public class ConveyorBlock extends Block implements ItemConsumer {

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

        boolean left = canConnect(tile, data.direction.rotate());
        boolean behind = canConnect(tile, data.direction.opposite());
        boolean right = canConnect(tile, data.direction.rotateCW());

        int frame = (int) ((factory.getTick() * speed() / 2)) % 8;

        TextureRegion[] textures;
        if (left && !behind && !right) {
            textures = turnFlipped;
        } else if (right && !behind && !left) {
            textures = turn;
        } else {
            textures = straight;
        }

        Utils.draw(textures[frame], data.drawX(), data.drawY(), data.direction.angle);
    }

    private boolean canConnect(Tile tile, Direction vec) {
        Tile side = tile.adjacent(vec);
        if (side == null) {
            return false;
        }

        BlockData b = side.getBlockData();

        if (b == null) {
            return false;
        }

        return side.getBlock() instanceof ConveyorBlock && b.direction == vec.opposite();
    }

    @Override
    public void tick(BlockData data) {
        Data d = (Data) data;
    }

    @Override
    public void onBlockBuild(Tile tile) {
        ConveyorManager.newConveyor(tile);
    }

    @Override
    public BlockData createData() {
        return new Data();
    }


    /**
     * speed of 1: items move by 1 pixel every tick
     */
    public float speed() {
        return 2f;
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public boolean acceptItem(Tile tile, Item item) {
        return ((Data) tile.getBlockData()).section.acceptItem(item);
    }

    @Override
    public void passItem(Tile tile, Item item) {
        ((Data) tile.getBlockData()).section.passItem(item);
    }

    public static class Data extends BlockData {

        public ConveyorManager.ConveyorSection section;
        public Tile previousTile; // null if not in the same section

        public boolean isStartOfSection() {
            return section.isStartOfSection(tile);
        }
    }
}
