package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.RelativeDirection.*;

public interface IConveyorBlock {

    enum Connection {
        INPUT,
        OUTPUT
    }

    Connection[] connections = new Connection[4];


    float speed();

    boolean passItem(Tile tile, Direction input, Item item);

    /**
     * @param inputPos input position relative to conveyor direction
     */
    boolean canTakeItemFrom(ConveyorData data, RelativeDirection inputPos);

    default boolean canTakeItemFrom(ConveyorData data, Direction blockDirection, Direction inputPos) {
        return canTakeItemFrom(data, relative(blockDirection, inputPos));
    }

    boolean canOutputTo(ConveyorData data, RelativeDirection outputPos);

    default boolean canOutputTo(ConveyorData data, Direction blockDirection, Direction inputPos) {
        return canOutputTo(data, relative(blockDirection, inputPos));
    }

    Connection canConnectWith(ConveyorData block, Direction direction);

    default Connection[] connections(ConveyorData block) {
        connections(block, connections);
        return connections;
    }

    default void connections(ConveyorData block, Connection[] out) {
        out[FACING.ordinal()] = canConnectWith(block, block.direction);
        out[LEFT.ordinal()] = canConnectWith(block, block.direction.rotate());
        out[RIGHT.ordinal()] = canConnectWith(block, block.direction.rotateCW());
        out[BEHIND.ordinal()] = canConnectWith(block, block.direction.opposite());
    }

    ConveyorManager.ConveyorSection newInput(ConveyorData block, RelativeDirection inputPos);

    void inputRemoved(ConveyorData block, RelativeDirection inputPos);

    ConveyorData createData(Tile tile);
}
