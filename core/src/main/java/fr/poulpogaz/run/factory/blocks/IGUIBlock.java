package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.math.Rectangle;
import fr.poulpogaz.run.factory.Tile;

import static fr.poulpogaz.run.Variables.TILE_SIZE;

public interface IGUIBlock {

    Rectangle showGUI(Tile tile);

    void drawGUI(Tile tile, Rectangle size);

    boolean updateGUI(Tile tile, Rectangle size);

    static Rectangle setDefaultGUIPosition(Tile tile, Rectangle rectangle) {
        Tile t = tile.multiBlockAnchor();

        rectangle.x = t.drawX() + TILE_SIZE * t.getBlock().width();
        rectangle.y = t.drawY();

        return rectangle;
    }
}
