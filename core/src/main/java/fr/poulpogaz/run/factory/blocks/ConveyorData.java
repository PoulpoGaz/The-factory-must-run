package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.item.Item;

public abstract class ConveyorData extends BlockData {

    public abstract float speed();

    /**
     * @return map in this order: left, right and finally behind to a priority (0 to 2)
     */
    public abstract int[] inputPriorities();

    public abstract void updateInputPriority(RelativeDirection choice);

    /**
     * where the item should go
     */
    public abstract RelativeDirection outputPriority(Item item, int attempt);

    /**
     * update output priorities
     */
    public abstract void updateOutputPriority(int attempt);

    public abstract ConveyorManager.ConveyorSection conveyorSection(RelativeDirection direction);

    public abstract void setConveyorSection(RelativeDirection direction, ConveyorManager.ConveyorSection section);
}
