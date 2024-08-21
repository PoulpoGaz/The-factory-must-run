package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

public interface ItemConsumer {

    boolean acceptItem(Tile tile, Item item);

    void passItem(Tile tile, Item item);
}
