package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;
import fr.poulpogaz.run.factory.item.Items;
import fr.poulpogaz.run.factory.recipes.Recipes;

import static fr.poulpogaz.run.Variables.*;

public class GeneratorBlock extends Block implements IGUIBlock {

    public GeneratorBlock(String name) {
        super(name);
    }

    @Override
    public void load() {
        super.load();
        ItemGUI.load();
    }

    @Override
    public void draw(Tile tile) {
        super.draw(tile);

        Data data = (Data) tile.getBlockData();
        if (data.item != null) {
            batch.draw(data.item.getIcon(),
                       tile.drawX() + HALF_TILE_SIZE / 2f,
                       tile.drawY() + HALF_TILE_SIZE / 2f);
        }
    }

    @Override
    public void tick(BlockData blockData) {
        if (factory.tick % 16 == 0) {
            Data data = (Data) blockData;

            if (data.item != null && (playerResources >= data.item.value || bankruptcyContinue)) {
                Tile tile = data.tile;

                for (Direction direction : Direction.values) {
                    generateItem(data, tile.adjacent(direction), direction);
                }
            }
        }
    }

    private void generateItem(Data genData, Tile adjacent, Direction rot) {
        if (adjacent != null && adjacent.getBlock() instanceof IConveyorBlock) {
            IConveyorBlock block = (IConveyorBlock) adjacent.getBlock();

            if (block.passItem(adjacent, rot, genData.item) && playerResources >= genData.item.value && !bankruptcyContinue) {
                playerResources -= genData.item.value;
            }
        }
    }

    @Override
    public BlockData createData(Tile tile) {
        return new Data();
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
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public int value() {
        return 50;
    }

    @Override
    public Rectangle showGUI(Tile tile) {
        return ItemGUI.showGUI(tile, Items.generable);
    }

    @Override
    public void drawGUI(Tile tile, Rectangle size) {
        HoverInterceptor.instance.index = -1;
        ItemGUI.drawGUI(tile, size, Items.generable, ((Data) tile.getBlockData()).item, HoverInterceptor.instance);
        HoverInterceptor.instance.draw();
    }

    @Override
    public boolean updateGUI(Tile tile, Rectangle size) {
        Item item = ItemGUI.updateGUI(tile, size, Items.generable);

        if (item != null) {
            Data data = (Data) tile.getBlockData();

            if (data.item == item) {
                data.item = null;
            } else {
                data.item = item;
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


        @Override
        public void itemHovered(float x, float y, int index, boolean hovered) {
            super.itemHovered(x, y, index, hovered);

            this.x = x;
            this.y = y;
            this.index = index;
        }

        public void draw() {
            if (index < 0) {
                return;
            }

            Item item = Items.generable.get(index);

            String text = Integer.toString(item.value);
            Utils.layout.setText(font, text);

            float length = Utils.layout.width + 2;

            float color = batch.getPackedColor();
            batch.setColor(0.3f, 0.3f, 0.3f, 0.8f);
            batch.draw(ItemGUI.white, x, y - HALF_TILE_SIZE - 2, length, HALF_TILE_SIZE + 2);
            batch.setPackedColor(color);

            float textY = y - 3;
            font.draw(batch, text, x + 1, textY);

        }
    }


    private static class Data extends BlockData {

        private Item item;
    }
}
