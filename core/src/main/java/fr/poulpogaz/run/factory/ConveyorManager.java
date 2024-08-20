package fr.poulpogaz.run.factory;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import fr.poulpogaz.run.Rotation;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.blocks.Block;
import fr.poulpogaz.run.factory.blocks.BlockData;
import fr.poulpogaz.run.factory.blocks.ConveyorBlock;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.Variables.*;

public class ConveyorManager {

    private static final Pool<ItemList> itemLists = Utils.newPool(ItemList::new);

    private static final Array<ConveyorSection> sections = new Array<>();
    private static int updatePass = 0;

    public static void newConveyor(Tile conveyor) {
        ConveyorBlock block = (ConveyorBlock) conveyor.getBlock();
        ConveyorBlock.Data data = (ConveyorBlock.Data) conveyor.getBlockData();

        Tile facing = conveyor.adjacent(data.rotation);
        Tile left = conveyor.adjacent(data.rotation.rotate());
        Tile right = conveyor.adjacent(data.rotation.rotateCW());
        Tile behind = conveyor.adjacent(data.rotation.opposite());

        boolean added = false;
        if (canConnectBefore(data.rotation, facing)) {
            appendBefore(conveyor, facing);
            added = true;
        }

        if (canConnectAfter(data.rotation.rotate(), left)) {
            appendAfter(conveyor, left);
            added = true;
        }

        if (canConnectAfter(data.rotation.rotateCW(), right)) {
            appendAfter(conveyor, right);
            added = true;
        }

        if (canConnectAfter(data.rotation.opposite(), behind)) {
            appendAfter(conveyor, behind);
            added = true;
        }

        if (!added) {
            createNewSection(conveyor);
        }
    }

    private static boolean canConnectBefore(Rotation vec, Tile tile) {
        Block block = tile.getBlock();
        BlockData data = tile.getBlockData();

        return block instanceof ConveyorBlock
            && data.rotation == vec;
    }

    private static boolean canConnectAfter(Rotation vec, Tile tile) {
        Block block = tile.getBlock();
        BlockData data = tile.getBlockData();

        return block instanceof ConveyorBlock
            && data.rotation == vec.opposite();
    }

    private static void appendAfter(Tile conveyor, Tile end) {
        ConveyorBlock.Data data = (ConveyorBlock.Data) end.getBlockData();

        data.section.appendAfter(conveyor);
    }

    private static void appendBefore(Tile conveyor, Tile end) {
        ConveyorBlock.Data data = (ConveyorBlock.Data) end.getBlockData();

        data.section.appendBefore(conveyor);
    }


    private static void createNewSection(Tile conveyor) {
        System.out.println("Creating new conveyor section");

        ConveyorSection section = new ConveyorSection();
        section.length = TILE_SIZE;
        section.firstTile = conveyor;
        section.endTile = conveyor;
        section.conveyorSpeed = ((ConveyorBlock) conveyor.getBlock()).speed();
        ((ConveyorBlock.Data) conveyor.getBlockData()).section = section;

        sections.add(section);
    }

    public static void tick() {
        updatePass++;

        for (int i = 0; i < sections.size; i++) {
            sections.get(i).update();
        }
    }

    private static final Vector2 temp = new Vector2();

    public static void drawItems() {
        for (int i = 0; i < sections.size; i++) {
            ConveyorSection section = sections.get(i);
            if (section.firstItem == null) {
                continue;
            }

            // traverse two linked list simultaneously
            // iterate over the item and find the tile in which
            // the current item is
            ItemList list = section.firstItem;
            Tile tile = section.firstTile;

            // distance between the current tile and the next item
            float length = 0;

            // rotation to use to draw items at the beginning of a conveyor
            Rotation rotation = null;

            while (list != null) {
                length += list.remaining;

                if (length > TILE_SIZE) {
                    // find the tile
                    while (length > TILE_SIZE) {
                        tile = ((ConveyorBlock.Data) tile.getBlockData()).previousTile;
                        length -= TILE_SIZE;
                    }
                    rotation = null;
                }

                if (rotation == null) {
                    Tile previous = ((ConveyorBlock.Data) tile.getBlockData()).previousTile;
                    if (previous != null) {
                        rotation = previous.getBlockData().rotation;
                    } else {
                        rotation = tile.getBlockData().rotation;
                    }
                }

                // choose the rotation depending on the position of the item
                Rotation r;
                if (rotation == null || length < HALF_TILE_SIZE) {
                    r = tile.getBlockData().rotation; // end of the conveyor
                } else {
                    r = rotation; // beginning of the conveyor
                }
                // position of the item relative to the center of the tile, un-rotated
                temp.set(HALF_TILE_SIZE - length, 0);
                r.rotate(temp);

                TextureRegion icon = list.item.getIcon();
                batch.draw(icon,
                           tile.drawX() + temp.x + HALF_TILE_SIZE / 2f,
                           tile.drawY() + temp.y + HALF_TILE_SIZE / 2f);

                list = list.previous;
            }
        }
    }

    public static void drawConveyorSections() {
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < sections.size; i++) {
            ConveyorSection section = sections.get(i);

            shape.setColor(Color.GREEN);
            Tile tile = section.firstTile;
            while (tile != null) {
                ConveyorBlock.Data data = (ConveyorBlock.Data) tile.getBlockData();

                float x = tile.drawX();
                float y = tile.drawY();

                int dx = data.rotation.dx;
                int dy = data.rotation.dy;

                Tile next = data.previousTile;
                if (next != null && next.getBlockData().rotation != data.rotation) {
                    float cx = x + HALF_TILE_SIZE;
                    float cy = y + HALF_TILE_SIZE;

                    shape.line(cx, cy, cx + dx * HALF_TILE_SIZE, cy + dy * HALF_TILE_SIZE);

                    int dx2 = next.getBlockData().rotation.opposite().dx;
                    int dy2 = next.getBlockData().rotation.opposite().dy;

                    shape.line(cx, cy, cx + dx2 * HALF_TILE_SIZE, cy + dy2 * HALF_TILE_SIZE);
                } else if (dx != 0) {
                    shape.line(x, y + HALF_TILE_SIZE, x + TILE_SIZE, y + HALF_TILE_SIZE);
                } else {
                    shape.line(x + HALF_TILE_SIZE, y, x + HALF_TILE_SIZE, y + TILE_SIZE);
                }

                tile = next;

                shape.setColor(Color.BLUE);
            }
        }
        shape.end();
        batch.begin();
    }

    public static class ConveyorSection {

        public static final int itemSpacing = 8;

        private ConveyorSection[] parents = new ConveyorSection[3];
        private ConveyorSection child;

        private Tile firstTile;
        private Tile endTile;

        private int lastUpdate;

        private ItemList firstItem;
        private ItemList firstNonBlockedItem;
        private ItemList lastItem;

        private float conveyorSpeed = 0;

        private float length;
        private float availableLength;

        public void update() {
            if (lastUpdate == updatePass || firstNonBlockedItem == null) {
                return;
            }

            lastUpdate = updatePass;

            int spacing = firstNonBlockedItem == firstItem ? 0 : itemSpacing;

            firstNonBlockedItem.remaining -= conveyorSpeed;
            if (firstNonBlockedItem.remaining < spacing) {
                firstNonBlockedItem.remaining = spacing;

                do {
                    firstNonBlockedItem = firstNonBlockedItem.previous;
                } while (firstNonBlockedItem != null && firstNonBlockedItem.remaining == spacing);
            } else {
                availableLength += conveyorSpeed;
            }
        }

        public void appendBefore(Tile before) {
            ((ConveyorBlock.Data) endTile.getBlockData()).previousTile = before;
            ((ConveyorBlock.Data) before.getBlockData()).section = this;
            endTile = before;

            length += TILE_SIZE;
            availableLength += TILE_SIZE;
        }

        public void appendAfter(Tile after) {
            ConveyorBlock.Data data = (ConveyorBlock.Data) after.getBlockData();
            data.previousTile = firstTile;
            data.section = this;
            firstTile = after;

            length += TILE_SIZE;

            if (firstItem != null) {
                firstItem.remaining += TILE_SIZE;
                firstNonBlockedItem = firstItem;
            } else {
                availableLength += TILE_SIZE;
            }
        }

        public void passItem(Item item) {
            if (!acceptItem(item)) {
                return;
            }

            ItemList list = itemLists.obtain();
            list.item = item;

            if (lastItem == null) {
                list.remaining = length;
                firstItem = list;
                firstNonBlockedItem = firstItem;
            } else {
                if (firstNonBlockedItem == null && availableLength > 0) {
                    firstNonBlockedItem = list;
                }

                list.remaining = availableLength;
                lastItem.previous = list;
            }
            lastItem = list;
            availableLength = 0;
        }

        public boolean acceptItem(Item item) {
            return availableLength >= itemSpacing;
        }
    }

    public static class ItemList {

        public Item item;
        public ItemList previous;
        public float remaining; // length between this item and the next one
    }
}
