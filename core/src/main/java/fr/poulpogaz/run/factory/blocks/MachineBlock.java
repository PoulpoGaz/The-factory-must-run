package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.factory.Tile;
import fr.poulpogaz.run.factory.recipes.Recipe;
import fr.poulpogaz.run.factory.recipes.Recipes;

import static fr.poulpogaz.run.Variables.*;

public class MachineBlock extends Block {

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
    public int width() {
        return 2;
    }

    @Override
    public int height() {
        return 2;
    }


    private static class Data extends BlockData {

        private Recipe recipe = Recipes.NOTHING;
    }
}
