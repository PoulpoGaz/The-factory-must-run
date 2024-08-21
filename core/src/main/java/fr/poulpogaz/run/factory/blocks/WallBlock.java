package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.factory.Tile;

import static fr.poulpogaz.run.Utils.flip;
import static fr.poulpogaz.run.Variables.atlas;

public class WallBlock extends Block implements AutoTiler {

    private TextureRegion[][] regions;

    public WallBlock() {
        super("wall");
    }

    @Override
    public void load() {
        super.load();

        TextureRegion wallDown = atlas.findRegion("wall_down");
        TextureRegion wallLeftRight = atlas.findRegion("wall_left_right");
        TextureRegion wallRight = atlas.findRegion("wall_right");
        TextureRegion wallTDown = atlas.findRegion("wall_t_down");
        TextureRegion wallTRight = atlas.findRegion("wall_t_right");
        TextureRegion wallTUp = atlas.findRegion("wall_t_up");
        TextureRegion wallTurnRight = atlas.findRegion("wall_turn_right");
        TextureRegion wallTurnUp = atlas.findRegion("wall_turn_up");
        TextureRegion wallUp = atlas.findRegion("wall_up");
        TextureRegion wallUpDown = atlas.findRegion("wall_up_down");
        TextureRegion wallX = atlas.findRegion("wall_x");

        regions = new TextureRegion[6][];
        regions[ALONE] = new TextureRegion[] {region};
        regions[END] = new TextureRegion[] {
            wallRight, wallUp, flip(wallRight, true, false), wallDown
        };
        regions[STRAIGHT] = new TextureRegion[] {wallLeftRight, wallUpDown};
        regions[T_INTERSECTION] = new TextureRegion[] {
            wallTRight, wallTUp, flip(wallTRight, true, false), wallTDown
        };
        regions[X_INTERSECTION] = new TextureRegion[] {wallX};
        regions[TURN] = new TextureRegion[] {
            wallTurnRight, wallTurnUp, flip(wallTurnUp, true, false), flip(wallTurnRight, true, false)
        };
    }

    @Override
    public void draw(Tile tile) {
        draw(tile, regions);
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
    public boolean connect(Tile tile, Tile other) {
        return other != null && other.getBlock() instanceof WallBlock;
    }
}
