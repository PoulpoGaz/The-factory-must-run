package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.Utils.loadAnimation;
import static fr.poulpogaz.run.Variables.factory;

public class RouterBlock extends Block {

    private TextureRegion[] regions;

    public RouterBlock(String name) {
        super(name);
    }

    @Override
    public void load() {
        super.load();

        regions = loadAnimation(name);
    }

    @Override
    public void draw(Tile tile) {
        BlockData data = tile.getBlockData();

        int frame = (int) ((factory.getTick() * speed() / 2)) % 8;
        Utils.draw(regions[frame], data.drawX(), data.drawY(), data.direction.angle);
    }

    /**
     * speed of 1: items move by 1 pixel every tick
     */
    public float speed() {
        return 2f;
    }

    @Override
    public BlockData createData(Tile tile) {
        return new Data((RouterBlock) tile.getBlock());
    }

    @Override
    public boolean isConveyor() {
        return true;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public int height() {
        return 1;
    }

    private class Data extends ConveyorData {

        public final RouterBlock block;

        public Data(RouterBlock block) {
            this.block = block;
        }

        @Override
        public float speed() {
            return block.speed();
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public int outputCount() {
            return 3;
        }

        @Override
        public int[] inputPriorities() {
            return new int[0];
        }

        @Override
        public void updateInputPriority(RelativeDirection choice) {

        }

        @Override
        public RelativeDirection outputPriority(Item item, int attempt) {
            return RelativeDirection.values[attempt];
        }

        @Override
        public void updateOutputPriority() {

        }
    }
}
