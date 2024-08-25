package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.Variables;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.RelativeDirection.*;
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
        Data data = (Data) tile.getBlockData();
        Connection[] connections = connections(data);

        ConveyorManager.ConveyorSection out;
        if (connections[BEHIND.ordinal()] == Connection.INPUT) {
            out = ConveyorManager.adjacentSection(data, BEHIND);
            out.growEnd(data, HALF_TILE_SIZE);
        } else if (connections[FACING.ordinal()] == Connection.OUTPUT) {
            out = ConveyorManager.adjacentSection(data, FACING);
            out.growFirst(data, HALF_TILE_SIZE);
        } else {
            out = new ConveyorManager.ConveyorSection()
                .init(data, data.isInput() ? BEHIND : FACING, data.isInput());
        }

        data.setConveyorSection(FACING, out);
    }

    @Override
    public void onBlockDestroyed(Tile tile) {
        Data data = (Data) tile.getBlockData();
        Connection[] connections = connections(data);

        ConveyorManager.ConveyorSection out = data.outputSection;

        boolean connected = false;
        if (connections[BEHIND.ordinal()] == Connection.INPUT) {
            if (data.isLinked()) {
                ConveyorManager.ConveyorSection parent = out.splitAt(data, false);
                parent.shrinkEnd(HALF_TILE_SIZE);
            } else {
                out.shrinkEnd(HALF_TILE_SIZE);
            }

            connected = true;
        } else if (connections[FACING.ordinal()] == Connection.OUTPUT) {
            ConveyorManager.ConveyorSection parent = out;
            if (data.isLinked()) {
                parent = out.splitAt(data, false);
            }
            out.shrinkStart(HALF_TILE_SIZE);
            ConveyorData facingData = (ConveyorData) data.adjacentRelative(FACING).getBlockData();
            IConveyorBlock facingBlock = (IConveyorBlock) facingData.tile.getBlock();
            facingBlock.inputRemoved(facingData, relativePos(facingData, data));

            out = parent;
            connected = true;
        }

        if (data.isLinked()) {
            int distance = (Math.abs(tile.x - data.linkedWith.tile.x)
                + Math.abs(tile.y - data.linkedWith.tile.y)) * TILE_SIZE;
            if (!connected) {
                distance += HALF_TILE_SIZE;
            }

            if (data.isInput()) {
                out.shrinkStart(distance, false);
                out.firstBlock = data.linkedWith;

            } else {
                out.shrinkEnd(distance, false);
                out.endBlock = data.linkedWith;
            }

            data.linkedWith.color = Color.WHITE;
            data.linkedWith.linkedWith = null;
            data.linkedWith = null;
            data.color = Color.WHITE;
        } else if (!connected) {
            out.removeSection();
        }


        if (Variables.input.selectedUndergroundConveyor != null
            && Variables.input.selectedUndergroundConveyor.getBlockData() == data) {
            Variables.input.selectedUndergroundConveyor = null;
        }
    }

    @Override
    public Data createData(Tile tile) {
        return new Data(this);
    }

    @Override
    public float speed() {
        return 2f;
    }

    public int range() {
        return 5;
    }

    /**
     * in range if distance between 'tile' center and 'other' center is less than range()
     */
    public boolean isInRange(Tile tile, Tile other) {
        return isInRange(tile.x, tile.y, other.x, other.y);
    }

    public boolean isInRange(int conveyorX, int conveyorY, int x, int y) {
        int dx = conveyorX - x;
        int dy = conveyorY - y;

        return Math.abs(dx) + Math.abs(dy) <= range() - 0.5;
    }

    public boolean link(Tile a, Tile b) {
        if (isInRange(a, b) && a.getBlockData() instanceof Data && b.getBlockData() instanceof Data) {
            Data aData = (Data) a.getBlockData();
            Data bData = (Data) b.getBlockData();

            if (aData.linkedWith == bData) {
                return true;
            }
            if (aData.linkedWith != null) {
                unlink(aData.tile);
            }
            if (bData.linkedWith != null) {
                unlink(bData.tile);
            }

            if (aData.isInput() && bData.isOutput()) {
                link(aData, bData);
                return true;
            } else if (aData.isOutput() && bData.isInput()) {
                link(bData, aData);
                return true;
            }
        }

        return false;
    }

    private void link(Data input, Data output) {
        int dx = output.drawX() - input.drawX();
        int dy = output.drawY() - input.drawY();

        ConveyorManager.ConveyorSection inSec = input.outputSection;

        if (dx != 0) {
            inSec.appendAfterEnd(dx > 0 ? Direction.RIGHT : Direction.LEFT, Math.abs(dx), true);
        }
        if (dy != 0) {
            inSec.appendAfterEnd(dy > 0 ? Direction.UP : Direction.DOWN, Math.abs(dy), true);
        }

        ConveyorManager.merge(inSec, output.outputSection);

        input.linkedWith = output;
        output.linkedWith = input;

        Color color = generateColor(input.tile);
        input.color = color;
        output.color = color;
    }

    private Color generateColor(Tile tile) {
        int linked = 0;
        for (int y = -range(); y <= range(); y++) {
            for (int x = -range(); x <= range(); x++) {
                int x1 = tile.x + x;
                int y1 = tile.y + y;

                Tile t = factory.getTile(x1, y1);

                if (!isInRange(tile.x, tile.y, x1, y1) || t == null) {
                    continue;
                }

                BlockData d = t.getBlockData();
                if (d instanceof Data) {
                    Data data = (Data) d;
                    if (data.isLinked()) {
                        linked++;

                        if (isInRange(tile, data.linkedWith.tile) && data.isInput()) {
                            linked--;
                        }
                    }
                }
            }
        }

        int next = MathUtils.nextPowerOfTwo(linked + 1);
        int prev = next >> 1;

        int i = linked - prev;
        int subDiv = next - prev;

        float hue = 360f * i / subDiv + 180f / subDiv;

        Color c = new Color();
        c.a = 1;
        return c.fromHsv(hue, 1, 1);
    }

    public boolean unlink(Tile tile) {
       if (tile.getBlockData() instanceof Data) {
           Data d = (Data) tile.getBlockData();

           if (d.linkedWith != null) {
               if (d.linkedWith.isInput()) {
                   unlink(d.linkedWith, d);
               } else {
                   unlink(d, d.linkedWith);
               }
               return true;
           }
       }

       return false;
    }

    private void unlink(Data input, Data output) {
        int distance = (Math.abs(input.tile.x - output.tile.x)
            + Math.abs(input.tile.y - output.tile.y)) * TILE_SIZE;

        ConveyorManager.ConveyorSection parent = output.outputSection.splitAt(output, false);
        input.outputSection.shrinkEnd(distance, false);
        input.outputSection.endBlock = input;
        input.outputSection = parent;

        input.linkedWith.color = Color.WHITE;
        input.linkedWith.linkedWith = null;
        input.linkedWith = null;
        input.color = Color.WHITE;

    }

    public boolean areLinked(Tile a, Tile b) {
        return a.getBlockData() instanceof Data
            && b.getBlockData() instanceof Data
            && ((Data) a.getBlockData()).linkedWith == b.getBlockData();
    }

    @Override
    public boolean passItem(Tile tile, Direction input, Item item) {
        Data data = (Data) tile.getBlockData();

        if (data.input && input == data.direction) {
            return data.outputSection.passItem(item);
        }

        return false;
    }

    @Override
    public boolean canTakeItemFrom(ConveyorData data, RelativeDirection inputPos) {
        return inputPos == BEHIND && ((Data) data).isInput();
    }

    @Override
    public boolean canOutputTo(ConveyorData data, RelativeDirection outputPos) {
        return outputPos == FACING && ((Data) data).isOutput();
    }

    @Override
    public Connection canConnectWith(ConveyorData block, Direction direction) {
        Tile adj = block.adjacent(direction);
        if (adj == null) {
            return null;
        }

        Block adjB = adj.getBlock();
        if (!adjB.isConveyor()) {
            return null;
        }
        IConveyorBlock adjConv = (IConveyorBlock) adjB;
        ConveyorData adjData = (ConveyorData) adj.getBlockData();

        Data data = (Data) block;

        if (data.direction == direction.opposite() && data.isInput()) {
            // behind
            if (adjConv.canOutputTo(adjData, adjData.direction, direction.opposite())) {
                return Connection.INPUT;
            }
        } else if (adjConv.canTakeItemFrom(adjData, adjData.direction, direction.opposite()) && data.isOutput()) {
            return Connection.OUTPUT;
        }

        return null;
    }

    @Override
    public ConveyorManager.ConveyorSection newInput(ConveyorData block, RelativeDirection inputPos) {
        return null;
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

    @Override
    public int value() {
        return 1000;
    }

    public static class Data extends ConveyorData implements IFlipData {

        private final UndergroundConveyorBlock block;
        private boolean input;
        private Color color = Color.WHITE;

        private Data linkedWith;

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
            if (direction == RelativeDirection.FACING || direction == RelativeDirection.BEHIND) {
                return outputSection;
            } else {
                return null;
            }
        }

        @Override
        public void setConveyorSection(RelativeDirection direction, ConveyorManager.ConveyorSection section) {
            if (direction == RelativeDirection.FACING) {
                this.outputSection = section;
            }
        }

        public boolean isInput() {
            return input;
        }

        public boolean isOutput() {
            return !input;
        }

        public boolean isLinked() {
            return linkedWith != null;
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
