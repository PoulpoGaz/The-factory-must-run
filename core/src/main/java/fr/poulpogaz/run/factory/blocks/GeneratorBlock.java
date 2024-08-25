package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.math.Rectangle;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.item.Item;
import fr.poulpogaz.run.factory.item.Items;

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

            if (data.item != null) {
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

            if (block.passItem(adjacent, rot, genData.item)) {
                playerResources += genData.item.value;
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
        ItemGUI.drawGUI(tile, size, Items.generable, ((Data) tile.getBlockData()).item);
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



    private static class Data extends BlockData {

        private Item item;
    }
}
