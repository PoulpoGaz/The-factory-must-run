package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Items;

import static fr.poulpogaz.run.Variables.factory;

public class GeneratorBlock extends Block {

    public GeneratorBlock(String name) {
        super(name);
    }

    @Override
    public void tick(BlockData data) {
        if (factory.tick % 30 == 0) {
            Tile tile = data.tile;

            for (Direction direction : Direction.values) {
                generateItem(tile.adjacent(direction), direction);
            }
        }
    }

    private void generateItem(Tile adjacent, Direction rot) {
        if (adjacent.getBlock() instanceof ConveyorBlock) {
            ConveyorBlock.Data data = (ConveyorBlock.Data) adjacent.getBlockData();

            if (data.direction == rot) {
                data.section.passItem(Items.IRON_PLATE);
            }
        }
    }

    @Override
    public BlockData createData() {
        return new BlockData();
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public int height() {
        return 1;
    }
}
