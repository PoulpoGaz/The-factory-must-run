package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;

import static fr.poulpogaz.run.RelativeDirection.*;

public interface IConveyorBlock {

    enum Connection {
        INPUT,
        OUTPUT
    }


    float speed();

    int inputCount(ConveyorData conveyor);

    int outputCount(ConveyorData conveyor);

    ConveyorManager.ConveyorSection createNewSection(Tile conveyor);

    /**
     * @param inputPos input position relative to conveyor direction
     */
    boolean canTakeItemFrom(RelativeDirection inputPos);

    default boolean canTakeItemFrom(Direction blockDirection, Direction inputPos) {
        return canTakeItemFrom(relative(blockDirection, inputPos));
    }

    boolean canOutputTo(RelativeDirection outputPos);

    default boolean canOutputTo(Direction blockDirection, Direction inputPos) {
        return canOutputTo(relative(blockDirection, inputPos));
    }

    Connection canConnectWith(ConveyorData block, Direction direction);

    default void connections(ConveyorData block, Connection[] out) {
        out[FACING.ordinal()] = canConnectWith(block, block.direction);
        out[LEFT.ordinal()] = canConnectWith(block, block.direction.rotate());
        out[RIGHT.ordinal()] = canConnectWith(block, block.direction.rotateCW());
        out[BEHIND.ordinal()] = canConnectWith(block, block.direction.opposite());
    }

    default boolean forceOutput(RelativeDirection dir) {
        return false;
    }

    ConveyorData createData(Tile tile);
}
