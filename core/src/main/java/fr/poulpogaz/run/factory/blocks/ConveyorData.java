package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.item.Item;

public abstract class ConveyorData extends BlockData {

    public ConveyorManager.ConveyorSection section;
    public ConveyorData previousBlock; // null if not in the same section

    public abstract float speed();

    public abstract int inputCount();

    public boolean hasMultipleInputs() {
        return inputCount() > 1;
    }

    public abstract int outputCount();

    public boolean hasMultipleOutputs() {
        return outputCount() > 1;
    }

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
    public abstract void updateOutputPriority();

    public ConveyorManager.ConveyorSection conveyorSection() {
        return section;
    }

    public boolean isSectionStart() {
        return section.isSectionStart(this);
    }

    public boolean isSectionEnd() {
        return section.isSectionEnd(this);
    }
}
