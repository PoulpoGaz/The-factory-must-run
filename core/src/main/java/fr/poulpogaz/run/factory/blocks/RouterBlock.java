package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

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
        ConveyorManager.newConveyor(tile);
    }

    /**
     * speed of 1: items move by 1 pixel every tick
     */
    @Override
    public float speed() {
        return 2f;
    }

    @Override
    public int inputCount(ConveyorData conveyor) {
        return 1;
    }

    @Override
    public int outputCount(ConveyorData conveyor) {
        return 3;
    }

    @Override
    public ConveyorManager.ConveyorSection createNewSection(Tile conveyor) {
        ConveyorData data = (ConveyorData) conveyor.getBlockData();

        ConveyorManager.ConveyorSection start = setupSection(conveyor);
        start.firstBlock.setConveyorSection(RelativeDirection.BEHIND, start);
        start.graph = createGraph(conveyor.drawX() + HALF_TILE_SIZE - data.direction.dx * HALF_TILE_SIZE,
                                  conveyor.drawY() + HALF_TILE_SIZE - data.direction.dy * HALF_TILE_SIZE,
                                  data.direction);
        start.childrenCount = 3;

        for (RelativeDirection d : RelativeDirection.values) {
            if (d == RelativeDirection.BEHIND) {
                continue;
            }

            ConveyorManager.ConveyorSection child = setupSection(conveyor);
            child.graph = createGraph(conveyor.drawX() + HALF_TILE_SIZE, conveyor.drawY() + HALF_TILE_SIZE, d.absolute(data.direction));
            child.parentCount = 1;
            child.parents[RelativeDirection.BEHIND.ordinal() - 1] = start;
            start.children[d.ordinal()] = child;
        }

        return start;
    }

    private ConveyorManager.ConveyorSection setupSection(Tile conveyor) {
        ConveyorManager.ConveyorSection section = new ConveyorManager.ConveyorSection();
        section.firstBlock = (ConveyorData) conveyor.getBlockData();
        section.endBlock = section.firstBlock;
        section.availableLength = HALF_TILE_SIZE;
        section.length = HALF_TILE_SIZE;

        return section;
    }

    private ConveyorManager.BlockGraph createGraph(int x, int y, Direction dir) {
        ConveyorManager.BlockGraph g = new ConveyorManager.BlockGraph();
        g.x = x;
        g.y = y;
        g.start = new ConveyorManager.BlockNode();
        g.end = g.start;
        g.start.length = HALF_TILE_SIZE;
        g.start.direction = dir;

        return g;
    }

    @Override
    public boolean canTakeItemFrom(RelativeDirection inputPos) {
        return inputPos == RelativeDirection.BEHIND;
    }

    @Override
    public boolean canOutputTo(RelativeDirection outputPos) {
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
            if (adjConv.canOutputTo(adjData.direction, direction.opposite())) {
                return Connection.INPUT;
            }
        } else {// if (adjConv.canTakeItemFrom(adjData.direction, direction.opposite())) {
            return Connection.OUTPUT;
        }

        return null;
    }

    @Override
    public Data createData(Tile tile) {
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

        private static final int[] inputPrio = new int[3];

        public final RouterBlock block;
        private ConveyorManager.ConveyorSection inputSection;

        private static final int[] outputPrio = {0, 1, 2};

        public Data(RouterBlock block) {
            this.block = block;
        }

        @Override
        public float speed() {
            return block.speed();
        }

        @Override
        public int[] inputPriorities() {
            return inputPrio;
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
            int o = outputPrio[0];
            outputPrio[0] = outputPrio[1];
            outputPrio[1] = outputPrio[2];
            outputPrio[2] = o;
        }

        @Override
        public ConveyorManager.ConveyorSection conveyorSection(RelativeDirection direction) {
            return direction == RelativeDirection.BEHIND ? inputSection : inputSection.getChild(direction);
        }

        @Override
        public void setConveyorSection(RelativeDirection direction, ConveyorManager.ConveyorSection section) {
            if (direction == RelativeDirection.BEHIND) {
                this.inputSection = section;
            } else {
                this.inputSection = section.getChild(direction);

                if (inputSection == null) {
                    inputSection = section;
                }
            }
        }
    }
}
