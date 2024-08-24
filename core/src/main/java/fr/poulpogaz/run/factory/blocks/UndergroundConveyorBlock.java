package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.Utils.loadAnimation;
import static fr.poulpogaz.run.Variables.*;

public class UndergroundConveyorBlock extends Block implements IConveyorBlock {

    private TextureRegion[] input;
    private TextureRegion[] output;
    private TextureRegion colorMask;

    public UndergroundConveyorBlock(String name) {
        super(name);
    }

    @Override
    public void load() {
        input = loadAnimation(name + "_input");
        output = loadAnimation(name + "_output");
        colorMask = atlas.findRegion(name + "_mask");

        region = input[0];
    }

    @Override
    public void draw(Tile tile) {
        Data data = (Data) tile.getBlockData();
        int frame = (int) ((factory.getTick() * speed() / 2)) % 8;

        TextureRegion[] textures = data.input ? input : output;

        Utils.draw(textures[frame], data.drawX(), data.drawY(), data.direction.angle);

        float packed = batch.getPackedColor();
        batch.setColor(data.color);
        Utils.draw(colorMask, data.drawX(), data.drawY(), data.direction.angle);
        batch.setPackedColor(packed);
    }

    @Override
    public void drawBuildPlan(float x, float y, Direction direction, boolean flipped) {
        float color = batch.getPackedColor();
        batch.setColor(1, 1, 1, 0.5f);

        TextureRegion r = flipped ? output[0] : input[0];
        Utils.draw(r, x, y, canBeRotated() ? direction.angle : 0);

        batch.setPackedColor(color);
    }

    @Override
    public void onBlockBuild(Tile tile) {

    }

    @Override
    public Data createData(Tile tile) {
        return new Data(this);
    }

    @Override
    public float speed() {
        return 2f;
    }

    @Override
    public boolean passItem(Tile tile, Direction input, Item item) {
        return false;
    }

    @Override
    public boolean canTakeItemFrom(ConveyorData data, RelativeDirection inputPos) {
        return inputPos == RelativeDirection.BEHIND && ((Data) data).isInput();
    }

    @Override
    public boolean canOutputTo(ConveyorData data, RelativeDirection outputPos) {
        return outputPos == RelativeDirection.BEHIND && ((Data) data).isOutput();
    }

    @Override
    public Connection canConnectWith(ConveyorData block, Direction direction) {
        Data data = (Data) block;

        if (data.direction == direction && data.isInput()) {
            return Connection.INPUT;
        } else if (data.direction == direction.opposite() && data.isOutput()) {
            return Connection.OUTPUT;
        }

        return null;
    }

    @Override
    public ConveyorManager.ConveyorSection newInput(ConveyorData block, RelativeDirection inputPos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void inputRemoved(ConveyorData block, RelativeDirection inputPos) {

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
    public boolean canBeRotated() {
        return true;
    }

    @Override
    public boolean canBeFlipped() {
        return true;
    }

    @Override
    public boolean isConveyor() {
        return true;
    }

    public static class Data extends ConveyorData implements IFlipData {

        public final UndergroundConveyorBlock block;
        private boolean input;
        private Color color = Color.RED;

        private ConveyorManager.ConveyorSection outputSection;

        public Data(UndergroundConveyorBlock block) {
            this.block = block;
        }

        @Override
        public float speed() {
            return block.speed();
        }

        @Override
        public int inputPriority(Item item, RelativeDirection dir) {
            return 0;
        }

        @Override
        public void updateInputPriority(RelativeDirection choice) {

        }

        @Override
        public RelativeDirection outputPriority(Item item, int attempt) {
            return RelativeDirection.FACING;
        }

        @Override
        public void updateOutputPriority(int attempt) {

        }

        @Override
        public ConveyorManager.ConveyorSection conveyorSection(RelativeDirection direction) {
            if (direction == RelativeDirection.BEHIND) {
                return outputSection;
            } else {
                return null;
            }
        }

        @Override
        public void setConveyorSection(RelativeDirection direction, ConveyorManager.ConveyorSection section) {
            if (direction == RelativeDirection.BEHIND) {
                outputSection = section;
            }
        }

        public boolean isInput() {
            return input;
        }

        public boolean isOutput() {
            return !input;
        }

        @Override
        public boolean isFlipped() {
            return isOutput();
        }

        @Override
        public void setFlipped(boolean flipped) {
            input = !flipped;
        }
    }
}
