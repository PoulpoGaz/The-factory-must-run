package fr.poulpogaz.run.factory.blocks;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import fr.poulpogaz.run.factory.Icon;
import fr.poulpogaz.run.factory.Tile;

import static fr.poulpogaz.run.Variables.*;

public class ItemGUI {

    private static final int ITEM_MARGIN = 2;
    private static final int ITEM_BOX_SIZE = HALF_TILE_SIZE + 2 * ITEM_MARGIN;
    private static final int OFFSET = 2;

    private static boolean loaded = false;

    public static TextureRegion white;
    public static TextureRegion itemSelected;

    public static void load() {
        if (loaded) {
            return;
        }

        loaded = true;
        white = atlas.findRegion("white");
        itemSelected = atlas.findRegion("item_selected");
    }

    public static Rectangle showGUI(Tile tile, Array<?> items) {
        int count = items.size;
        int iW = Math.min(count, 5);
        int iH = (count - 1) / 5 + 1;

        int width = 2 * OFFSET + iW * ITEM_BOX_SIZE;
        int height = 2 * OFFSET + iH * ITEM_BOX_SIZE;

        return IGUIBlock.setDefaultGUIPosition(tile, new Rectangle(0, 0, width, height));
    }

    public static <T extends Icon> void drawGUI(Tile tile, Rectangle size, Array<T> items, T selectedItem) {
        drawGUI(tile, size, items, selectedItem, DefaultHoverInterceptor.instance);
    }

    public static <T extends Icon> void drawGUI(Tile tile, Rectangle size, Array<T> items, T selectedItem, HoverInterceptor interceptor) {
        float color = batch.getPackedColor();
        batch.setColor(0.3f, 0.3f, 0.3f, 0.8f);
        batch.draw(white, size.x, size.y, size.width, size.height);
        batch.setPackedColor(color);

        int hoverI = hoveredItemIndex(size);

        float x = size.x + OFFSET;
        float y = size.y + size.height - OFFSET - ITEM_BOX_SIZE;
        for (int i = 0; i < items.size; i++) {
            T item = items.get(i);
            batch.draw(item.getIcon(), x + ITEM_MARGIN, y + ITEM_MARGIN);

            if (item == selectedItem) {
                batch.draw(itemSelected, x, y);
            }

            if (hoverI == i) {
                interceptor.itemHovered(x, y, hoverI, item == selectedItem);
            }

            x += ITEM_BOX_SIZE;
            if (x + ITEM_BOX_SIZE > size.x + size.width) {
                y -= ITEM_BOX_SIZE;
                x = size.x + OFFSET;
            }
        }
    }

    public static <T> T updateGUI(Tile tile, Rectangle size, Array<T> items) {
        if (input.isMouseJustPressed(Input.Buttons.LEFT)) {
            int i = hoveredItemIndex(size);

            if (i >= 0 && i < items.size) {
                return items.get(i);
            }
        }

        return null;
    }


    private static int hoveredItemIndex(Rectangle guiSize) {
        float gX = guiSize.x + OFFSET;
        float gY = guiSize.y + OFFSET;
        float gX2 = gX + guiSize.width - 2 * OFFSET;
        float gY2 = gY + guiSize.height - 2 * OFFSET;

        if (input.world.x >= gX
            && input.world.y >= gY
            && input.world.x <= gX2
            && input.world.y <= gY2) {

            float rx = input.world.x - guiSize.x - OFFSET;
            float ry = guiSize.height - 2 * OFFSET - (input.world.y - guiSize.y - OFFSET);

            int x = (int) rx / ITEM_BOX_SIZE;
            int y = (int) ry / ITEM_BOX_SIZE;

            return y * 5 + x;
        }

        return -1;
    }

    public interface HoverInterceptor {
        void itemHovered(float x, float y, int index, boolean selected);
    }

    public static class DefaultHoverInterceptor implements HoverInterceptor {

        public static final DefaultHoverInterceptor instance = new DefaultHoverInterceptor();

        @Override
        public void itemHovered(float x, float y, int index, boolean selected) {
            if (!selected) {
                float color = batch.getPackedColor();
                batch.setColor(0.5f, 0.5f, 0.5f, 1f);
                batch.draw(itemSelected, x, y);
                batch.setPackedColor(color);
            }
        }
    }
}
