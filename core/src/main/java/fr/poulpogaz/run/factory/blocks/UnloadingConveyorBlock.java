package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.factory.item.Item;
import fr.poulpogaz.run.factory.item.Items;

public class UnloadingConveyorBlock extends Block {

    public UnloadingConveyorBlock(String name) {
        super(name);
    }

    @Override
    public BlockData createData() {
        return super.createData();
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public int height() {
        return 1;
    }

    public static class Data extends BlockData {

        public Item source = Items.GEAR;
    }
}
