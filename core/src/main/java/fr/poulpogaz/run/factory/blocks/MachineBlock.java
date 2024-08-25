package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.ConveyorManager;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;
import fr.poulpogaz.run.factory.recipes.Recipe;
import fr.poulpogaz.run.factory.recipes.Recipes;

import java.io.DataInput;
import java.util.Arrays;

import static fr.poulpogaz.run.Variables.*;

public class MachineBlock extends Block implements ItemConsumer {

    public MachineBlock() {
        super("machine");
    }

    @Override
    public void load() {
        super.load();
    }

    @Override
    public void draw(Tile tile) {
        super.draw(tile);

        Data data = (Data) tile.getBlockData();
        TextureRegion icon = data.recipe.icon;
        batch.draw(icon,
                   tile.drawX() + HALF_TILE_SIZE, tile.drawY() + HALF_TILE_SIZE,
                   TILE_SIZE, TILE_SIZE);
    }

    @Override
    public BlockData createData(Tile tile) {
        return new Data();
    }

    @Override
    public void tick(BlockData data) {
        Data machine = (Data) data;

        if (machine.isManufacturing()) {
            machine.tick++;

            if (machine.tick >= machine.recipeDuration()) {
                machine.finishManufacturing();
            }
        }

        if (machine.canManufacture()) {
            machine.startManufacturing();
        }

        if (machine.hasFinishedProducts()) {
            MultiBlockAdjacentIterator it = MultiBlockAdjacentIterator.instance;
            it.set(data.tile);

            Item item = machine.firstOutputItem();
            while (it.hasNext()) {
                Tile adj = it.next();
                boolean succ = outputTo(adj, it.normal, item);

                if (succ) {
                    machine.removeItemOutput(item);
                    if (!machine.hasFinishedProducts()) {
                        break;
                    }

                    item = machine.firstOutputItem();
                }
            }
        }
    }

    private boolean outputTo(Tile adj, Direction dir, Item item) {
        Block adjBlock = adj.getBlock();
        if (adjBlock instanceof ItemConsumer) {
            // TODO
            return false;
        } else if (adjBlock instanceof IConveyorBlock) {
            IConveyorBlock conveyor = (IConveyorBlock) adjBlock;
            return conveyor.passItem(adj, dir, item);
        }

        return false;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public int width() {
        return 2;
    }

    @Override
    public int height() {
        return 2;
    }

    @Override
    public boolean acceptItem(Tile tile, Item item) {
        Data data = (Data) tile.getBlockData();
        int itemIndex = data.indexOfInput(item);

        return itemIndex >= 0 && data.contents[itemIndex] < 2 * data.recipe.inputs[itemIndex].count;
    }

    @Override
    public void passItem(Tile tile, Item item) {
        Data data = (Data) tile.getBlockData();
        data.addItemInput(item);
    }

    private static class Data extends BlockData {

        private Recipe recipe;

        private int[] contents;
        private int validIngredientsCount;
        private int outputFullCount;
        private int outputAvailableCount;

        private int tick;
        private boolean manufacturing = false;

        public Data() {
            setRecipe(Recipes.GEAR);
        }

        public void setRecipe(Recipe recipe) {
            this.recipe = recipe;
            contents = new int[recipe.inputCount() + recipe.outputCount()];
        }

        public void addItemInput(Item item) {
            int itemIndex = indexOfInput(item);

            if (contents[itemIndex] < 2 * recipe.requiredCount(itemIndex)) {
                contents[itemIndex]++;

                if (contents[itemIndex] == recipe.requiredCount(itemIndex)) {
                    validIngredientsCount++;
                }
            }
        }

        public void removeItemOutput(Item item) {
            int i = indexOfOutput(item);
            if (i < 0) {
                throw new IllegalStateException();
            }

            int itemIndex = recipe.inputCount() + i;

            contents[itemIndex]--;
            if (contents[itemIndex] == 0) {
                outputAvailableCount--;
            } else if (contents[itemIndex] == 2 * recipe.outputCount(itemIndex - recipe.inputCount())) {
                outputFullCount--;
            }
        }

        public Item firstOutputItem() {
            for (int i = 0; i < recipe.outputCount(); i++) {
                if (contents[i + recipe.inputCount()] > 0) {
                    return recipe.outputs[i].item;
                }
            }

            return null;
        }

        public void startManufacturing() {
            if (canManufacture()) {
                System.out.println("Start Manufacturing...");

                for (int i = 0; i < recipe.inputCount(); i++) {
                    contents[i] -= recipe.requiredCount(i);

                    if (contents[i] < recipe.requiredCount(i)) {
                        validIngredientsCount--;
                    }
                }

                manufacturing = true;
            }
        }

        public void finishManufacturing() {
            if (tick < recipeDuration()) {
                return;
            }

            System.out.println("finished manufacturing");

            for (int i = 0, j = recipe.inputCount(); i < recipe.outputCount(); i++, j++) {
                if (contents[j] == 0) {
                    outputAvailableCount++;
                }

                contents[j] += recipe.outputCount(i);

                if (contents[j] > 2 * recipe.outputCount(i)) {
                    outputFullCount++;
                }
            }

            tick = 0;
            manufacturing = false;
        }

        public boolean canManufacture() {
            return !manufacturing
                && outputFullCount == 0
                && validIngredientsCount == recipe.inputCount();
        }

        public boolean hasFinishedProducts() {
            return outputAvailableCount > 0;
        }



        public int indexOfInput(Item item) {
            return recipe.indexOfInput(item);
        }

        public int indexOfOutput(Item item) {
            return recipe.indexOfOutput(item);
        }

        public boolean isManufacturing() {
            return manufacturing;
        }

        public int recipeDuration() {
            return recipe.duration;
        }
    }
}
