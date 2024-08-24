package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Items;

import java.lang.invoke.VarHandle;

import static fr.poulpogaz.run.Variables.factory;

public class GeneratorBlock extends Block {

    public GeneratorBlock(String name) {
        super(name);
    }

    @Override
    public void tick(BlockData data) {
        if (factory.getTick() % 1 == 0) {
            Tile tile = data.tile;

            for (Direction direction : Direction.values) {
                generateItem((Data) data, tile.adjacent(direction), direction);
            }
        }
    }

    private void generateItem(Data genData, Tile adjacent, Direction rot) {
        if (adjacent != null && adjacent.getBlock() instanceof IConveyorBlock) {
            IConveyorBlock block = (IConveyorBlock) adjacent.getBlock();

            block.passItem(adjacent, rot,
                           genData.i % 3 == 0 ? Items.IRON_PLATE : (genData.i % 3 == 1 ? Items.GEAR : Items.PIPE));
        }
    }

    @Override
    public BlockData createData(Tile tile) {
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

    @Override
    public boolean isUpdatable() {
        return true;
    }

    private static class Data extends BlockData {

        private static int count = 0;

        private int i;

        public Data() {
            this.i = count;
            count++;
            System.out.println(i);
        }
    }
}
