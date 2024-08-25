package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Variables;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

public class ConsumerBlock extends Block implements ItemConsumer {

    public ConsumerBlock() {
        super("consumer");
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
    public int value() {
        return 20;
    }

    @Override
    public boolean acceptItem(Tile tile, Item item) {
        return true;
    }

    @Override
    public void passItem(Tile tile, Item item) {
        Variables.playerResources += item.value;
    }
}
