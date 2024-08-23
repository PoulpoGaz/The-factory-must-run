package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.Utils.loadAnimation;
import static fr.poulpogaz.run.Variables.*;

public class ConveyorBlock extends Block implements ItemConsumer, IConveyorBlock {

    private TextureRegion[] straight;
    private TextureRegion[] turn;
    private TextureRegion[] turnFlipped;

    public ConveyorBlock(String name) {
        super(name);
    }

    @Override
    public void load() {
        super.load();

        straight = loadAnimation("conveyor");
        turn = loadAnimation("conveyor_turn");

        turnFlipped = new TextureRegion[turn.length];
        for (int i = 0; i < turn.length; i++) {
            turnFlipped[i] = Utils.flip(turn[i], false, true);
        }
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

        Block block = side.getBlock();
        if (!block.isConveyor()) {
            return false;
        }

        ConveyorData data = (ConveyorData) side.getBlockData();

        return data.direction == vec.opposite();
    }

    @Override
    public void onBlockBuild(Tile tile) {
        ConveyorManager.newConveyor(tile);
    }

    @Override
    public Data createData(Tile tile) {
        return new Data((ConveyorBlock) tile.getBlock());
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
        return 3;
    }

    @Override
    public int outputCount(ConveyorData conveyor) {
        return 1;
    }

    @Override
    public ConveyorManager.ConveyorSection createNewSection(Tile conveyor) {
        return new ConveyorManager.ConveyorSection(conveyor);
    }

    @Override
    public boolean canTakeItemFrom(RelativeDirection inputPos) {
        return inputPos != RelativeDirection.FACING;
    }

    public boolean canOutputTo(RelativeDirection outputPos) {
        return outputPos == RelativeDirection.FACING;
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

        if (block.direction == direction) {
            if (adjConv.canTakeItemFrom(adjData.direction, direction.opposite())) {
                return Connection.OUTPUT;
            }
        } else if (adjConv.canOutputTo(adjData.direction, direction.opposite())) {
            return Connection.INPUT;
        }

        return null;
    }

    @Override
    public boolean forceOutput(RelativeDirection dir) {
        return dir == RelativeDirection.FACING;
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
        return ((Data) tile.getBlockData()).outputSection.acceptItem(item);
    }

    @Override
    public void passItem(Tile tile, Item item) {
        ((Data) tile.getBlockData()).outputSection.passItem(item);
    }

    @Override
    public boolean canBeRotated() {
        return true;
    }

    @Override
    public boolean isConveyor() {
        return true;
    }

    public static class Data extends ConveyorData {


        public final ConveyorBlock block;
        private final int[] priorities = {0, 1, 2};
        private ConveyorManager.ConveyorSection outputSection;

        public Data(ConveyorBlock block) {
            this.block = block;
        }

        @Override
        public float speed() {
            return block.speed();
        }

        @Override
        public int[] inputPriorities() {
            return priorities;
        }

        @Override
        public void updateInputPriority(RelativeDirection choice) {
            int i = choice.ordinal() - 1;
            priorities[i] = 0;

            int i2 = (i + 1) % 3;
            int i3 = (i2 + 1) % 3;

            if (priorities[i3] == 2) {
                priorities[i2] = 1;
            } else if (priorities[i2] == 2) {
                priorities[i3] = 1;
            } else {
                priorities[i2]++;
                priorities[i3]++;
            }
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
            if (direction == RelativeDirection.FACING || !outputSection.hasParent()) {
                return outputSection;
            } else {
                return outputSection.getParent(direction);
            }
        }

        @Override
        public void setConveyorSection(RelativeDirection direction, ConveyorManager.ConveyorSection section) {
            if (direction != RelativeDirection.FACING) {
                throw new IllegalStateException();
            }

            outputSection = section;
        }
    }
}
