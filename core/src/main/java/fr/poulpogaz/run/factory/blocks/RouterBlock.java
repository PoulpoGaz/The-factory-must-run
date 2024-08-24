package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.RelativeDirection.*;
import static fr.poulpogaz.run.RelativeDirection.relativePos;
import static fr.poulpogaz.run.Utils.loadAnimation;
import static fr.poulpogaz.run.Variables.HALF_TILE_SIZE;
import static fr.poulpogaz.run.Variables.factory;

public class RouterBlock extends Block implements IConveyorBlock {

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

    @Override
    public void onBlockBuild(Tile tile) {
        ConveyorData data = (ConveyorData) tile.getBlockData();
        Connection[] connections = connections(data);

        ConveyorManager.ConveyorSection parent;
        if (connections[BEHIND.ordinal()] == Connection.INPUT) {
            parent = ConveyorManager.adjacentSection(data, BEHIND);
            parent.growEnd(data, HALF_TILE_SIZE);
        } else {
            parent = new ConveyorManager.ConveyorSection()
                .init(data, BEHIND, true);
        }

        data.setConveyorSection(BEHIND, parent);

        for (int i = 0; i < 3; i++) {
            ConveyorManager.ConveyorSection s = ConveyorManager.setupOutput(data, connections, values[i]);
            s.addParent(parent, BEHIND, values[i]);
        }
    }

    @Override
    public void onBlockDestroyed(Tile tile) {
        Data data = (Data) tile.getBlockData();

        ConveyorManager.ConveyorSection input = data.inputSection;
        for (int i = 0; i < 3; i++) {
            ConveyorManager.ConveyorSection sec = data.inputSection.getChild(values[i]);
            sec.removeParent(input, BEHIND, values[i]);

            if (sec.graph.length == HALF_TILE_SIZE) {
                sec.removeSection();
            } else {
                sec.shrinkStart(HALF_TILE_SIZE);

                ConveyorData adjData = (ConveyorData) data.adjacentRelative(values[i]).getBlockData();
                IConveyorBlock adjBlock = (IConveyorBlock) adjData.tile.getBlock();
                adjBlock.inputRemoved(adjData, relativePos(adjData, data));
            }
        }

        input.selectedInput = false;
        if (input.graph.length == HALF_TILE_SIZE) {
            input.removeSection();
        } else {
            input.shrinkEnd(HALF_TILE_SIZE);
        }
    }

    /**
     * speed of 1: items move by 1 pixel every tick
     */
    @Override
    public float speed() {
        return 2f;
    }

    @Override
    public boolean passItem(Tile tile, Direction input, Item item) {
        Data data = (Data) tile.getBlockData();

        if (data.direction == input) {
            data.inputSection.passItem(item);
            return true;
        }

        return false;
    }

    @Override
    public boolean canTakeItemFrom(ConveyorData data, RelativeDirection inputPos) {
        return inputPos == RelativeDirection.BEHIND;
    }

    @Override
    public boolean canOutputTo(ConveyorData data, RelativeDirection outputPos) {
        return outputPos != RelativeDirection.BEHIND;
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

        if (block.direction == direction.opposite()) {
            // behind
            if (adjConv.canOutputTo(adjData, adjData.direction, direction.opposite())) {
                return Connection.INPUT;
            }
        } else if (adjConv.canTakeItemFrom(adjData, adjData.direction, direction.opposite())) {
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
    public Data createData(Tile tile) {
        return new Data(this);
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
    public boolean canBeRotated() {
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

    public static class Data extends ConveyorData {

        public final RouterBlock block;
        private ConveyorManager.ConveyorSection inputSection;

        private final int[] outputPrio = {0, 1, 2};

        public Data(RouterBlock block) {
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
            return RelativeDirection.values[outputPrio[attempt]];
        }

        @Override
        public void updateOutputPriority(int attempt) {
            if (attempt == 0) {
                int o = outputPrio[0];
                outputPrio[0] = outputPrio[1];
                outputPrio[1] = outputPrio[2];
                outputPrio[2] = o;
            } else if (attempt == 1) {
                int o = outputPrio[1];
                outputPrio[1] = outputPrio[2];
                outputPrio[2] = o;
            }
        }

        @Override
        public ConveyorManager.ConveyorSection conveyorSection(RelativeDirection direction) {
            return direction == RelativeDirection.BEHIND ? inputSection : inputSection.getChild(direction);
        }

        @Override
        public void setConveyorSection(RelativeDirection direction, ConveyorManager.ConveyorSection section) {
            if (direction == RelativeDirection.BEHIND) {
                this.inputSection = section;
            }
        }
    }
}
