package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.RelativeDirection.*;
import static fr.poulpogaz.run.Utils.loadAnimation;
import static fr.poulpogaz.run.Variables.*;

public class ConveyorBlock extends Block implements IConveyorBlock {

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

        boolean left = canConnectWith(data, data.direction.rotate()) == Connection.INPUT;
        boolean behind = canConnectWith(data, data.direction.opposite()) == Connection.INPUT;
        boolean right = canConnectWith(data, data.direction.rotateCW()) == Connection.INPUT;

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

    @Override
    public void onBlockBuild(Tile tile) {
        ConveyorData data = (ConveyorData) tile.getBlockData();
        Connection[] connections = connections(data);

        ConveyorManager.ConveyorSection s = ConveyorManager.setupOutput(data, connections, FACING);

        if (Utils.count(connections, Connection.INPUT) > 1) {
            // link
            for (int i = 1; i < 4; i++) {
                if (connections[i] == Connection.INPUT) {
                    ConveyorManager.ConveyorSection parent = ConveyorManager.adjacentSection(data, values[i]);
                    RelativeDirection r = relativePos(data, parent.endBlock);
                    parent.growEnd(data, HALF_TILE_SIZE);
                    s.addParent(parent, r, FACING);
                }
            }

        } else {
            // merge
            ConveyorManager.ConveyorSection input = null;
            for (int i = 1; i < 4; i++) {
                if (connections[i] == Connection.INPUT) {
                    input = ConveyorManager.adjacentSection(data, values[i]);
                }
            }

            if (input == null) {
                s.appendBeforeFirst(data, HALF_TILE_SIZE);
            } else {
                input.appendAfterEnd(data, HALF_TILE_SIZE);
                ConveyorManager.merge(input, s);
            }
        }
    }

    @Override
    public void onBlockDestroyed(Tile tile) {
        Data data = (Data) tile.getBlockData();
        Connection[] connections = connections(data);

        ConveyorManager.ConveyorSection out = data.outputSection;
        boolean start = out.isSectionStart(data);

        int link = Utils.count(connections, null);

        if (link == 4) {
            out.removeSection();
        } else if (link == 3 || link == 2 && (!out.hasParent() || !start)) {
            // turn or straight

            if (connections[FACING.ordinal()] == Connection.OUTPUT && !start) {
                ConveyorManager.ConveyorSection before = out.splitAt(data, false);
                before.shrinkEnd(HALF_TILE_SIZE);
                out.shrinkStart(HALF_TILE_SIZE);

                ConveyorData facingData = (ConveyorData) data.adjacentRelative(FACING).getBlockData();
                IConveyorBlock facingBlock = (IConveyorBlock) facingData.tile.getBlock();
                facingBlock.inputRemoved(facingData, relativePos(facingData, data));
            } else if (start) {
                out.shrinkStart(TILE_SIZE);

                ConveyorData facingData = (ConveyorData) data.adjacentRelative(FACING).getBlockData();
                IConveyorBlock facingBlock = (IConveyorBlock) facingData.tile.getBlock();
                facingBlock.inputRemoved(facingData, relativePos(facingData, data));
            } else {
                out.shrinkEnd(TILE_SIZE);
            }
        } else {
            for (int i = 0; i < out.parents.length; i++) {
                ConveyorManager.ConveyorSection parent = out.parents[i];

                if (parent != null) {
                    parent.selectedInput = false;
                    parent.shrinkEnd(HALF_TILE_SIZE);
                    out.removeParent(parent, values[i + 1], FACING);
                }
            }

            if (out.graph.length == HALF_TILE_SIZE) {
                out.removeSection();
            } else {
                out.shrinkStart(HALF_TILE_SIZE);
            }
        }
    }

    @Override
    public Data createData(Tile tile) {
        return new Data(this);
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

        ConveyorManager.ConveyorSection s = data.outputSection;
        if (s != null && data.direction == input && s.isSectionStart(data) && !s.hasParent()) {
            return s.passItem(item);
        }

        return false;
    }

    @Override
    public boolean canTakeItemFrom(ConveyorData data, RelativeDirection inputPos) {
        return inputPos != RelativeDirection.FACING;
    }

    @Override
    public boolean canOutputTo(ConveyorData data, RelativeDirection outputPos) {
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
            if (adjConv.canTakeItemFrom(adjData, adjData.direction, direction.opposite())) {
                return Connection.OUTPUT;
            }
        } else if (adjConv.canOutputTo(adjData, adjData.direction, direction.opposite())) {
            return Connection.INPUT;
        }

        return null;
    }

    @Override
    public ConveyorManager.ConveyorSection newInput(ConveyorData block, RelativeDirection inputPos) {
        Data d = (Data) block;
        ConveyorManager.ConveyorSection out = d.outputSection;

        if (!out.isSectionStart(block)) {
            out.splitAt(block, true);
        }

        Direction dir = inputPos.absolute(block.direction);

        if (out.parentCount == 0) {
            out.graph.growStart(-HALF_TILE_SIZE);
            out.graph.addFirst(HALF_TILE_SIZE, dir.opposite(), false);

            return out;
        } else {
            ConveyorManager.ConveyorSection sec = new ConveyorManager.ConveyorSection();
            sec.firstBlock = block;
            sec.endBlock = block;
            sec.graph = new ConveyorManager.BlockGraph()
                .init(block.tile, dir, HALF_TILE_SIZE, dir.opposite());
            sec.availableLength = sec.graph.length;

            ConveyorManager.ConveyorSection child = ((Data) block).outputSection;
            child.parentCount++;
            child.parents[inputPos.ordinal() - 1] = sec;
            sec.childrenCount++;
            sec.children[FACING.ordinal()] = child;

            return sec;
        }
    }

    @Override
    public void inputRemoved(ConveyorData block, RelativeDirection inputPos) {
        Data d = (Data) block;
        ConveyorManager.ConveyorSection sec = d.outputSection;

        if (sec.parentCount >= 2) {
            ConveyorManager.ConveyorSection parent = sec.getParent(inputPos);
            sec.removeParent(parent, inputPos, FACING);
            parent.removeSection();

            if (sec.parentCount == 1) {
                ConveyorManager.merge(sec.getUniqueParent(), sec);
            }
        } else if (inputPos != BEHIND) {
            sec.graph.shrinkStart(HALF_TILE_SIZE);
            sec.graph.growStart(HALF_TILE_SIZE);
        }
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
        public int inputPriority(Item item, RelativeDirection dir) {
            return priorities[dir.ordinal() - 1];
        }

        @Override
        public void updateInputPriority(RelativeDirection choice) {
            int i = choice.ordinal() - 1;

            int i2 = (i + 1) % 3;
            int i3 = (i2 + 1) % 3;
            if (priorities[i] == 1) {
                if (priorities[i3] == 2) {
                    priorities[i3] = 1;
                } else if (priorities[i2] == 2) {
                    priorities[i2] = 1;
                }
            } else if (priorities[i] == 0) {
                priorities[i2]--;
                priorities[i3]--;
            }

            priorities[i] = 2;
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
            if (direction == RelativeDirection.FACING) {
                return outputSection;
            } else if (outputSection.hasParent()) {
                return outputSection.getParent(direction);
            } else if (block.canConnectWith(this, direction.absolute(this.direction)) == Connection.INPUT) {
                return outputSection;
            } else {
                return null;
            }
        }

        @Override
        public void setConveyorSection(RelativeDirection direction, ConveyorManager.ConveyorSection section) {
            if (direction == RelativeDirection.FACING) {
                outputSection = section;
            }
        }
    }
}
