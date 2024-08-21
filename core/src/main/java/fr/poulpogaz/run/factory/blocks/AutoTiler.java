package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.Tile;

import static fr.poulpogaz.run.Variables.batch;

public interface AutoTiler {

    int ALONE = 0;
    int END = 1;
    int STRAIGHT = 2;
    int T_INTERSECTION = 3;
    int X_INTERSECTION = 4;
    int TURN = 5;


    int[] array = new int[2];

    /**
     * regions:
     *  array[i] contains texture regions for when autoTile(...)[0] returns i
     *  array[i] is of length:
     *   - 1 if i == 0 (alone)
     *   - 2 if i == 2 (straight)
     *   - 4
     *  if array[i][j] is null, it draws array[i][0] rotated by j * 90 degrees
     */
    default void draw(Tile tile, TextureRegion[][] regions) {
        int[] tiles = autoTile(tile);

        TextureRegion r = regions[tiles[0]][tiles[1]];
        if (r == null) {
            r = regions[tiles[0]][0];
            Utils.draw(r, tile.drawX(), tile.drawY(), tiles[1] * 90);
        } else {
            batch.draw(r, tile.drawX(), tile.drawY());
        }
    }

    /**
     * array[0]:
     *  0 - alone
     *  1 - straight end
     *  2 - straight
     *  3 - t intersection
     *  4 - x intersection
     *  5 - turn
     * array[1]:
     *  rotation
     *    state 0/4 doesn't have a rotation
     */
    default int[] autoTile(Tile tile) {
        int up = connect(tile, tile.adjacent(0, 1)) ? 1 : 0;
        int down = connect(tile, tile.adjacent(0, -1)) ? 1 : 0;
        int right = connect(tile, tile.adjacent(1, 0)) ? 1 : 0;
        int left = connect(tile, tile.adjacent(-1, 0)) ? 1 : 0;

        array[0] = up + down + right + left;

        if (array[0] == 2 && up + down != 2 && right + left != 2) {
            array[0] = 5;
        }

        switch (array[0]) {
            case END:
                array[1] = right * 0 + up * 1 + left * 2 + down * 3;
                break;
            case STRAIGHT:
                array[1] = up;
                break;
            case T_INTERSECTION:
                array[1] = (1 - left) * 0 + (1 - down) * 1 + (1 - right) * 2 + (1 - up) * 3;
                break;
            case TURN:
                array[1] = 0 * right * down + 1 * up * right + 2 * left * up + 3 * down * left;
                break;
            default:
                array[1] = 0;
                break;
        }

        return array;
    }

    boolean connect(Tile tile, Tile other);
}
