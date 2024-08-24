package fr.poulpogaz.run.factory.blocks;

import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.factory.Tile;

public class AirBlock extends Block {

    public AirBlock() {
        super("air");
    }

    @Override
    public void load() {

    }

    @Override
    public void draw(Tile tile) {

    }

    @Override
    public void drawBuildPlan(float x, float y, Direction direction, boolean flipped) {

    }

    @Override
    public boolean isAir() {
        return true;
    }

    @Override
    public int width() {
        return 0;
    }

    @Override
    public int height() {
        return 0;
    }
}
