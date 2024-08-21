package fr.poulpogaz.run.factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.blocks.BlockData;
import fr.poulpogaz.run.factory.blocks.ConveyorBlock;
import fr.poulpogaz.run.factory.blocks.ItemConsumer;
import fr.poulpogaz.run.factory.item.Item;

import static fr.poulpogaz.run.Variables.*;

public class ConveyorManager {

    private static final Pool<ItemList> itemListPool = Utils.newPool(ItemList::new);

    private static final Array<ConveyorSection> sections = new Array<>();
    private static int updatePass = 0;

    public static void newConveyor(Tile conveyor) {
        ConveyorBlock block = (ConveyorBlock) conveyor.getBlock();
        ConveyorBlock.Data data = (ConveyorBlock.Data) conveyor.getBlockData();

        boolean added = false;
        if (canConnectWithFacing(conveyor)) {
            linkWithFacing(conveyor);
            added = true;
        }

        Direction d = data.direction.rotate();
        while (d != data.direction) {
            if (canConnectWithBehind(conveyor, d)) {
                linkWithBehind(conveyor, d);
                added = true;
            }

            d = d.rotate();
        }

        if (!added) {
            createNewSection(conveyor);
        }
    }

    /**
     * Can connect 'conveyor' with the tile right after 'conveyor'
     */
    private static boolean canConnectWithFacing(Tile conveyor) {
        BlockData data = conveyor.getBlockData();
        Tile after = conveyor.adjacent(data.direction);

        return after.getBlock() instanceof ConveyorBlock
            && after.getBlockData().direction != data.direction.opposite();
    }

    private static void linkWithFacing(Tile conveyor) {
        Tile facing = conveyor.adjacent(conveyor.getBlockData().direction);
        ConveyorBlock.Data facingData = (ConveyorBlock.Data) facing.getBlockData();

        ConveyorSection facingSection = facingData.section;

        if (facingData.isStartOfSection() && facingSection.parentsSize == 0) {
            facingSection.appendBefore(conveyor);
        } else {
            if (facingData.isStartOfSection() && facingSection.parentsSize == 0 || !facingData.isStartOfSection()) {
                // split
                Gdx.app.debug("Conveyor", "Splitting section at tile " + facing.x + ", " + facing.y);
                facingSection.splitAt(facing);
            }

            // create new conveyor section
            facingSection.addParent(new ConveyorSection(conveyor));
        }
    }

    /**
     * Can connect 'conveyor' with the adjacent tile int the direction 'behindDir'
     */
    private static boolean canConnectWithBehind(Tile conveyor, Direction behindDir) {
        Tile behind = conveyor.adjacent(behindDir);

        return behind.getBlock() instanceof ConveyorBlock
            && behind.getBlockData().direction == behindDir.opposite();
    }

    private static void linkWithBehind(Tile conveyor, Direction behindDir) {
        Tile behind = conveyor.adjacent(behindDir);
        ConveyorBlock.Data data = (ConveyorBlock.Data) behind.getBlockData();

        data.section.appendAfter(conveyor);
    }

    private static void createNewSection(Tile conveyor) {
        Gdx.app.debug("Conveyor", "Creating new conveyor section");

        sections.add(new ConveyorSection(conveyor));
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

            ItemIterator it = ItemIterator.instance;
            it.set(section);

            // rotation to use to draw items at the beginning of a conveyor
            Direction direction = null;

            Tile previousTile = null;
            while (it.hasNext()) {
                it.next();

                if (it.tile != previousTile) {
                    Tile previous = ((ConveyorBlock.Data) it.tile.getBlockData()).previousTile;
                    if (previous != null) {
                        direction = previous.getBlockData().direction;
                    } else {
                        direction = it.tile.getBlockData().direction;
                    }
                }

                // choose the rotation depending on the position of the item
                Direction r;
                if (direction == null || it.length < HALF_TILE_SIZE) {
                    r = it.tile.getBlockData().direction; // end of the conveyor
                } else {
                    r = direction; // beginning of the conveyor
                }
                // position of the item relative to the center of the tile, un-rotated
                temp.set(HALF_TILE_SIZE - it.length, 0);
                r.rotate(temp);

                TextureRegion icon = it.item().getIcon();
                batch.draw(icon,
                           it.tile.drawX() + temp.x + HALF_TILE_SIZE / 2f,
                           it.tile.drawY() + temp.y + HALF_TILE_SIZE / 2f);

                previousTile = it.tile;
            }
        }
    }

    public static void drawConveyorSections() {
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < sections.size; i++) {
            ConveyorSection section = sections.get(i);

            shape.setColor(Color.GREEN);
            Tile tile = section.endTile;
            while (tile != null) {
                ConveyorBlock.Data data = (ConveyorBlock.Data) tile.getBlockData();

                float x = tile.drawX();
                float y = tile.drawY();

                int dx = data.direction.dx;
                int dy = data.direction.dy;

                Tile next = data.previousTile;
                if (next != null && next.getBlockData().direction != data.direction) {
                    float cx = x + HALF_TILE_SIZE;
                    float cy = y + HALF_TILE_SIZE;

                    shape.line(cx, cy, cx + dx * HALF_TILE_SIZE, cy + dy * HALF_TILE_SIZE);

                    int dx2 = next.getBlockData().direction.opposite().dx;
                    int dy2 = next.getBlockData().direction.opposite().dy;

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

    /**
     * @return the tile before 'tile' in the conveyor section
     */
    private static Tile previous(Tile tile) {
        return ((ConveyorBlock.Data) tile.getBlockData()).previousTile;
    }

    public static class ConveyorSection {

        public static final int itemSpacing = 8;

        private final ConveyorSection[] parents = new ConveyorSection[3];
        private int parentsSize;
        private ConveyorSection child;

        private Tile firstTile; // tile in which items are added
        private Tile endTile;

        private int lastUpdate;

        private ItemList firstItem;
        private ItemList firstNonBlockedItem;
        private ItemList lastItem;

        private float conveyorSpeed = 0;

        private float length;
        private float availableLength;

        public ConveyorSection() {
        }

        // create a section with one conveyor
        public ConveyorSection(Tile conveyor) {
            length = TILE_SIZE;
            availableLength = TILE_SIZE;
            endTile = conveyor;
            firstTile = conveyor;
            conveyorSpeed = ((ConveyorBlock) conveyor.getBlock()).speed();
            ((ConveyorBlock.Data) conveyor.getBlockData()).section = this;
        }

        public void update() {
            if (lastUpdate == updatePass) {
                return;
            }

            lastUpdate = updatePass;

            // transfer item
            if (firstItem != null && firstItem.remaining == 0) {
                Tile next = endTile.adjacent(endTile.getBlockData().direction);
                if (next.getBlock() instanceof ItemConsumer) {
                    ItemConsumer c = (ItemConsumer) next.getBlock();

                    if (c.acceptItem(next, firstItem.item)) {
                        ItemList prev = firstItem.previous;
                        if (prev != null) {
                            prev.next = null;
                        } else {
                            lastItem = null;
                        }
                        Item item = firstItem.item;

                        itemListPool.free(firstItem);
                        c.passItem(next, item);

                        firstItem = prev;
                        firstNonBlockedItem = prev;

                    }
                }
            }

            if (firstNonBlockedItem == null) {
                return;
            }

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
            ((ConveyorBlock.Data) firstTile.getBlockData()).previousTile = before;
            ((ConveyorBlock.Data) before.getBlockData()).section = this;
            firstTile = before;

            length += TILE_SIZE;
            availableLength += TILE_SIZE;
        }

        public void appendAfter(Tile after) {
            ConveyorBlock.Data data = (ConveyorBlock.Data) after.getBlockData();
            data.previousTile = endTile;
            data.section = this;
            endTile = after;

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

            ItemList list = itemListPool.obtain();
            list.item = item;
            list.next = lastItem;
            list.previous = null;

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

        public boolean isStartOfSection(Tile tile) {
            return firstTile == tile;
        }

        public boolean isEndOfSection(Tile tile) {
            return endTile == tile;
        }

        public void splitAt(Tile tile) {
            if (tile == firstTile) {
                return; // create a 0-length section ?
            }

            // update parents/child
            ConveyorSection parent = new ConveyorSection();
            parent.conveyorSpeed = conveyorSpeed;
            parent.child = this;

            for (int i = 0; i < parentsSize; i++) {
                parents[i].child = parent;
            }

            parentsSize = 1;
            parents[0] = parent;

            // update first/end tiles
            parent.firstTile = firstTile;
            parent.endTile = previous(tile);
            firstTile = tile;


            // move items and update lengths
            TileIterator it = TileIterator.instance;
            it.set(this);
            it.moveTo(parent.endTile);

            parent.length = length - it.totalLength;
            length = it.totalLength;

            if (it.list == null) {
                // no items move to parent
                parent.availableLength = parent.length;

            } else if (it.list.next == null) {
                // all items move to parent

                parent.firstItem = firstItem;
                parent.lastItem = lastItem;
                parent.firstNonBlockedItem = firstItem;
                parent.availableLength = availableLength;

                availableLength = it.totalLength;
                firstItem = null;
                firstNonBlockedItem = null;
                lastItem = null;
            } else {
                // partial

                parent.firstItem = it.list;
                parent.lastItem = lastItem;
                parent.firstNonBlockedItem = firstItem;
                parent.availableLength = availableLength;

                lastItem = it.list.next;
                availableLength = it.length - it.list.remaining;
            }


            // update block data
            ((ConveyorBlock.Data) tile.getBlockData()).previousTile = null;

            Tile t = parent.endTile;
            while (t != null) {
                ConveyorBlock.Data data = (ConveyorBlock.Data) t.getBlockData();
                data.section = parent;
                t = data.previousTile;
            }

            sections.add(parent);
        }

        public void addParent(ConveyorSection section) {
            section.child = this;
            parents[parentsSize] = section;
            parentsSize++;

            sections.add(section);
        }
    }

    public static class ItemList {

        public Item item;
        public ItemList previous;
        public ItemList next;
        public float remaining; // length between this item and the next one
    }

    public static class TileIterator {

        private static final TileIterator instance = new TileIterator();

        private Tile tile;
        private ConveyorBlock.Data data;
        private ItemList list;

        // distance between the current tile and the next item
        // > 32 => not on the same tile
        public float length;
        // TILE_SIZE * (iterated tile count - 1)
        // doesn't include current tile
        public float totalLength;

        private boolean start = false;

        public void set(ConveyorSection section) {
            list = section.firstItem;
            tile = section.endTile;
            data = (ConveyorBlock.Data) tile.getBlockData();

            totalLength = 0;
            length = list == null ? -1 : list.remaining;

            start = true;
        }

        public void next() {
            if (!hasNext() || start) {
                start = false;
                return;
            }

            tile = data.previousTile;
            data = (ConveyorBlock.Data) tile.getBlockData();
            totalLength += TILE_SIZE;

            if (list != null) {
                length -= TILE_SIZE;

                while (length < 0 && list != null) {
                    length += list.remaining;
                    list = list.previous;
                }

                if (list == null) {
                    length = -1;
                }
            }
        }

        public boolean hasNext() {
            return start || data.previousTile != null;
        }

        public void moveTo(Tile tile) {
            while (hasNext() && this.tile != tile) {
                next();
            }
        }
    }

    /**
     * Iterates over the item of a section.
     */
    public static class ItemIterator {

        private static final ItemIterator instance = new ItemIterator();

        private static final ItemList initList = new ItemList();

        private Tile tile;
        private ItemList list;

        // distance between the current tile and the current item
        public float length;
        // TILE_SIZE * (iterated tile count - 1)
        // doesn't include current tile
        public float totalLength;

        public void set(ConveyorSection section) {
            list = initList;
            initList.previous = section.firstItem;
            tile = section.endTile;

            totalLength = 0;
            length = 0;
        }

        public void next() {
            if (!hasNext()) {
                return;
            }

            // traverse item's list
            list = list.previous;
            length += list.remaining;

            // find the tile
            while (length > TILE_SIZE) {
                tile = ((ConveyorBlock.Data) tile.getBlockData()).previousTile;
                length -= TILE_SIZE;
                totalLength += TILE_SIZE;
            }
        }

        public boolean hasNext() {
            return list.previous != null;
        }

        public Item item() {
            return list == null ? null : list.item;
        }
    }
}
