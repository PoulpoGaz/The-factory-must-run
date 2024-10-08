package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;
import fr.poulpogaz.run.factory.item.Items;

import static fr.poulpogaz.run.RelativeDirection.*;
import static fr.poulpogaz.run.Utils.loadAnimation;
import static fr.poulpogaz.run.Variables.*;

public class RouterBlock extends Block implements IConveyorBlock, IGUIBlock {

    private TextureRegion[] regions;

    public RouterBlock(String name) {
        super(name);
    }

    @Override
    public void load() {
        super.load();
        ItemGUI.load();
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
            return data.inputSection.passItem(item);
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
    public int value() {
        return 200;
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

    @Override
    public Rectangle showGUI(Tile tile) {
        return ItemGUI.showGUI(tile, Items.all);
    }

    @Override
    public void drawGUI(Tile tile, Rectangle size) {
        ItemGUI.drawGUI(tile, size, Items.all, ((Data) tile.getBlockData()).filter);
    }

    @Override
    public boolean updateGUI(Tile tile, Rectangle size) {
        Item item = ItemGUI.updateGUI(tile, size, Items.all);

        if (item != null) {
            Data data = (Data) tile.getBlockData();

            if (data.filter == item) {
                data.filter = null;
            } else {
                data.filter = item;
            }

            return true;
        }
        return false;
    }

    public static class Data extends ConveyorData {

        public final RouterBlock block;
        private ConveyorManager.ConveyorSection inputSection;

        private final int[] outputPrio = {0, 1, 2};

        private Item filter;

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
            if (filter == item) {
                return FACING;
            } else if (filter != null && outputPrio[attempt] == FACING.ordinal()) {
                return RelativeDirection.values[outputPrio[(attempt + 1) % 3]];
            } else {
                return RelativeDirection.values[outputPrio[attempt]];
            }
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

        public Item getFilter() {
            return filter;
        }
    }
}
