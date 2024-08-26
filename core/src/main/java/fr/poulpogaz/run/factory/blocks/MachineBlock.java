package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;
import fr.poulpogaz.run.factory.recipes.Recipe;
import fr.poulpogaz.run.factory.recipes.Recipes;

import java.util.Arrays;

import static fr.poulpogaz.run.Variables.*;

public class MachineBlock extends Block implements ItemConsumer, IGUIBlock {

    public MachineBlock() {
        super("machine");
    }

    @Override
    public void load() {
        super.load();
        ItemGUI.load();
        HoverInterceptor.instance.load();
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

        if (machine.recipe == Recipes.NOTHING) {
            return;
        }

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
            ItemConsumer c = (ItemConsumer) adjBlock;

            if (c.acceptItem(adj, item)) {
                c.passItem(adj, item);
                return true;
            }
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
    public int value() {
        return 200;
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

    @Override
    public Rectangle showGUI(Tile tile) {
        return ItemGUI.showGUI(tile, Recipes.all);
    }

    @Override
    public void drawGUI(Tile tile, Rectangle size) {
        HoverInterceptor.instance.index = -1;
        ItemGUI.drawGUI(tile, size, Recipes.all, ((Data) tile.getBlockData()).recipe, HoverInterceptor.instance);
        HoverInterceptor.instance.draw();
    }

    @Override
    public boolean updateGUI(Tile tile, Rectangle size) {
        Recipe recipe = ItemGUI.updateGUI(tile, size, Recipes.all);

        if (recipe != null) {
            Data data = (Data) tile.getBlockData();
            if (data.recipe != recipe) {
                data.setRecipe(recipe);
            }

            return true;
        }

        return false;
    }



    private static class HoverInterceptor extends ItemGUI.DefaultHoverInterceptor {

        private static final HoverInterceptor instance = new HoverInterceptor();

        private float x;
        private float y;
        private int index;

        private TextureRegion clock;
        private TextureRegion arrow;

        public void load() {
            clock = atlas.findRegion("clock");
            arrow = atlas.findRegion("arrow");
        }

        @Override
        public void itemHovered(float x, float y, int index, boolean hovered) {
            super.itemHovered(x, y, index, hovered);

            this.x = x;
            this.y = y;
            this.index = index;
        }

        public void draw() {
            if (index < 0 || Recipes.all.get(index) == Recipes.NOTHING) {
                return;
            }

            Recipe recipe = Recipes.all.get(index);
            float length = layout(recipe);

            float color = batch.getPackedColor();
            batch.setColor(0.3f, 0.3f, 0.3f, 0.8f);
            batch.draw(ItemGUI.white, x, y - HALF_TILE_SIZE - 2, length, HALF_TILE_SIZE + 2);
            batch.setPackedColor(color);

            float textY = y - 3;
            float imageY = y - HALF_TILE_SIZE - 1;

            x++;
            x += font.draw(batch, Float.toString(recipe.duration / 60f), x, textY).width + 1;
            batch.draw(clock, x, imageY);
            x += clock.getRegionWidth() + 1;

            for (int i = 0; i < recipe.inputCount(); i++) {
                x += font.draw(batch, Integer.toString(recipe.requiredCount(i)), x, textY).width + 1;
                batch.draw(recipe.inputs[i].item.getIcon(), x, imageY);
                x += HALF_TILE_SIZE + 1;
            }

            batch.draw(arrow, x, imageY);
            x += clock.getRegionWidth() + 1;

            for (int i = 0; i < recipe.outputCount(); i++) {
                x += font.draw(batch, Integer.toString(recipe.outputCount(i)), x, textY).width + 1;
                batch.draw(recipe.outputs[i].item.getIcon(), x, imageY);
                x += HALF_TILE_SIZE + 1;
            }
        }

        private float layout(Recipe recipe) {
            Utils.layout.setText(font, Float.toString(recipe.duration / 60f));

            float length = Utils.layout.width + clock.getRegionWidth() + 2;
            for (int i = 0; i < recipe.inputCount(); i++) {
                Utils.layout.setText(font, Integer.toString(recipe.requiredCount(i)));
                length += Utils.layout.width;
                length += HALF_TILE_SIZE + 2;
            }

            length += arrow.getRegionWidth();

            for (int i = 0; i < recipe.outputCount(); i++) {
                Utils.layout.setText(font, Integer.toString(recipe.outputCount(i)));
                length += Utils.layout.width;
                length += HALF_TILE_SIZE + 2;
            }

            return length + 1;
        }
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
            setRecipe(Recipes.NOTHING);
        }

        public void setRecipe(Recipe recipe) {
            this.recipe = recipe;

            int length = recipe.inputCount() + recipe.outputCount();
            if (contents != null && length == contents.length) {
                Arrays.fill(contents, 0);
            } else {
                contents = new int[length];
            }

            validIngredientsCount = 0;
            outputFullCount = 0;
            outputAvailableCount = 0;
            tick = 0;
            manufacturing = false;
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
