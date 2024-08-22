package fr.poulpogaz.run.factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.blocks.*;
import fr.poulpogaz.run.factory.item.Item;

import java.util.Arrays;

import static fr.poulpogaz.run.RelativeDirection.*;
import static fr.poulpogaz.run.Variables.*;

public class ConveyorManager {

    private static final Pool<ItemList> itemListPool = Utils.newPool(ItemList::new);

    private static final Array<ConveyorSection> sections = new Array<>();
    private static int updatePass = 0;

    public static void newConveyor(Tile conveyor) {
        ConveyorData data = (ConveyorData) conveyor.getBlockData();

        boolean linkedWithFacing = false;
        if (canConnectWithFacing(conveyor)) {
            linkWithFacing(conveyor);
            System.out.println("LINK FACING");
            linkedWithFacing = true;
        }

        boolean linkedWithBehind = false;
        Direction d = data.direction.rotate();
        while (d != data.direction) {
            if (canConnectWithBehind(conveyor, d)) {
                if (!linkedWithBehind) {
                    if (linkedWithFacing) {
                        merge(adjSection(conveyor, d), data.section);
                        System.out.println("MERGE: " + d);
                    } else {
                        System.out.println("LINK BEHIND: " + d);
                        linkWithBehind(conveyor, d);
                    }
                    linkedWithBehind = true;
                } else {
                    if (!data.isSectionStart()) {
                        System.out.println("SPLIT");
                        data.section.splitAt(data);
                    }
                    System.out.println("ADD PARENT: " + d);
                    data.section.addParent(adjSection(conveyor, d));
                }
            }

            d = d.rotate();
        }

        if (!linkedWithFacing && !linkedWithBehind) {
            createNewSection(conveyor);
        }
    }

    private static ConveyorSection adjSection(Tile tile, Direction direction) {
        return ((ConveyorData) tile.adjacent(direction).getBlockData()).section;
    }

    /**
     * Can connect 'conveyor' with the tile right after 'conveyor'
     */
    private static boolean canConnectWithFacing(Tile conveyor) {
        BlockData data = conveyor.getBlockData();
        Tile after = conveyor.adjacent(data.direction);

        return after.getBlock().isConveyor()
            && after.getBlockData().direction != data.direction.opposite();
    }

    private static void linkWithFacing(Tile conveyor) {
        Tile facing = conveyor.adjacent(conveyor.getBlockData().direction);
        ConveyorData facingData = (ConveyorData) facing.getBlockData();

        ConveyorSection facingSection = facingData.section;

        if (facingData.isSectionStart() && facingSection.parentCount == 0) {
            facingSection.appendBefore((ConveyorData) conveyor.getBlockData());
        } else {
            if (facingData.isSectionStart() && facingSection.parentCount == 0 || !facingData.isSectionStart()) {
                // split
                Gdx.app.debug("Conveyor", "Splitting section at tile " + facing.x + ", " + facing.y);
                facingSection.splitAt(facingData);
            }

            // create new conveyor section
            ConveyorSection s = new ConveyorSection(conveyor);
            sections.add(s);
            facingSection.addParent(s);
        }
    }

    /**
     * Can connect 'conveyor' with the adjacent tile int the direction 'behindDir'
     */
    private static boolean canConnectWithBehind(Tile conveyor, Direction behindDir) {
        Tile behind = conveyor.adjacent(behindDir);

        return behind.getBlock().isConveyor()
            && behind.getBlockData().direction == behindDir.opposite();
    }

    private static void linkWithBehind(Tile conveyor, Direction behindDir) {
        Tile behind = conveyor.adjacent(behindDir);
        ConveyorBlock.Data data = (ConveyorBlock.Data) behind.getBlockData();

        data.section.appendAfter((ConveyorData) conveyor.getBlockData());
    }

    private static void createNewSection(Tile conveyor) {
        Gdx.app.debug("Conveyor", "Creating new conveyor section");

        sections.add(new ConveyorSection(conveyor));
    }

    /**
     * Merge 'before' and 'after'.
     * 'before' will be removed
     */
    private static void merge(ConveyorSection before, ConveyorSection after) {
        if (before.endBlock.facing() != after.firstBlock.tile
            && after.firstBlock.direction != before.endBlock.direction) {
            throw new IllegalStateException("Cannot merge: disconnected sections");
        }

        if (before.childrenCount > 0) {
            throw new IllegalStateException("'before' should have zero child");
        }
        if (after.parentCount > 0) {
            throw new IllegalStateException("'after' should have zero parent");
        }

        after.firstBlock.previousBlock = before.endBlock;
        after.firstBlock = before.firstBlock;
        // update parents
        copyParentsTo(before, after);

        // move items and update lengths
        if (before.firstItem != null && after.firstItem != null) {
            after.lastItem.previous = before.firstItem;
            before.firstItem.next = after.lastItem;
            before.firstItem.remaining += after.availableLength;
            after.lastItem = before.lastItem;
            after.firstNonBlockedItem = after.firstItem;
            after.availableLength = before.availableLength;
        } else if (before.firstItem != null) {
            after.firstItem = before.firstItem;
            after.firstNonBlockedItem = before.firstItem;
            after.lastItem = before.lastItem;
            after.firstItem.remaining += after.length;
            after.availableLength = before.availableLength;
        } else {
            // no item to move
            after.availableLength += before.availableLength;
        }
        after.length += before.length;

        // update block data
        ConveyorData b = before.endBlock;
        while (b != null) {
            b.section = after;
            b = b.previousBlock;
        }

        sections.removeValue(before, true);
    }

    private static void copyParentsTo(ConveyorSection src, ConveyorSection dest) {
        dest.parentCount = src.parentCount;
        System.arraycopy(src.parents, 0, dest.parents, 0, 3);
        for (int i = 0; i < 3; i++) {
            ConveyorSection parPar = dest.parents[i];
            if (parPar != null) {
                RelativeDirection rc = relativePos(parPar.endBlock, dest.firstBlock);
                int ic = rc.ordinal();
                parPar.children[ic] = dest;
            }
        }

        Arrays.fill(src.parents, null);
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

            ConveyorData previousBlock = null;
            while (it.hasNext()) {
                it.next();

                if (it.block != previousBlock) {
                    ConveyorData previous = it.block.previousBlock;
                    if (previous != null) {
                        direction = previous.direction;
                    } else {
                        direction = it.block.direction;
                    }
                }

                // choose the rotation depending on the position of the item
                Direction r;
                if (direction == null || it.length < HALF_TILE_SIZE) {
                    r = it.block.direction; // end of the conveyor
                } else {
                    r = direction; // beginning of the conveyor
                }
                // position of the item relative to the center of the tile, un-rotated
                temp.set(HALF_TILE_SIZE - it.length, 0);
                r.rotate(temp);

                TextureRegion icon = it.item().getIcon();
                batch.draw(icon,
                           it.block.drawX() + temp.x + HALF_TILE_SIZE / 2f,
                           it.block.drawY() + temp.y + HALF_TILE_SIZE / 2f);

                previousBlock = it.block;
            }
        }
    }

    public static void drawConveyorSections() {
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < sections.size; i++) {
            ConveyorSection section = sections.get(i);

            shape.setColor(Color.GREEN);
            ConveyorData block = section.endBlock;
            while (block != null) {
                float x = block.drawX();
                float y = block.drawY();

                int dx = block.direction.dx;
                int dy = block.direction.dy;

                ConveyorData next = block.previousBlock;
                if (next != null && next.direction != block.direction) {
                    float cx = x + HALF_TILE_SIZE;
                    float cy = y + HALF_TILE_SIZE;

                    shape.line(cx, cy, cx + dx * HALF_TILE_SIZE, cy + dy * HALF_TILE_SIZE);

                    int dx2 = next.direction.opposite().dx;
                    int dy2 = next.direction.opposite().dy;

                    shape.line(cx, cy, cx + dx2 * HALF_TILE_SIZE, cy + dy2 * HALF_TILE_SIZE);
                } else if (dx != 0) {
                    shape.line(x, y + HALF_TILE_SIZE, x + TILE_SIZE, y + HALF_TILE_SIZE);
                } else {
                    shape.line(x + HALF_TILE_SIZE, y, x + HALF_TILE_SIZE, y + TILE_SIZE);
                }

                block = next;

                shape.setColor(Color.BLUE);
            }
        }
        shape.end();
        batch.begin();
    }

    public static class ConveyorSection {

        public static final int itemSpacing = 8;

        // parents[0] -> left
        // parents[1] -> right
        // parents[2] -> behind
        // => RelativeDirection.ordinal() - 1
        private final ConveyorSection[] parents = new ConveyorSection[3];
        private int parentCount;
        // children[0] -> facing
        // children[1] -> left
        // children[2] -> right
        // => RelativeDirection.ordinal()
        private final ConveyorSection[] children = new ConveyorSection[3];
        private int childrenCount;

        private ConveyorData firstBlock; // block in which items are added
        private ConveyorData endBlock;

        private int lastUpdate;

        private ItemList firstItem;
        private ItemList firstNonBlockedItem;
        private ItemList lastItem;

        private float length;
        private float availableLength;

        private boolean selectedInput = false;

        public ConveyorSection() {
        }

        // create a section with one conveyor
        public ConveyorSection(Tile conveyor) {
            length = TILE_SIZE;
            availableLength = TILE_SIZE;
            firstBlock = (ConveyorData) conveyor.getBlockData();
            endBlock = firstBlock;
            firstBlock.section = this;
        }

        public void update() {
            if (lastUpdate == updatePass) {
                return;
            }

            lastUpdate = updatePass;

            selectInput();

            // transfer item
            if (firstItem != null && firstItem.remaining == 0) {
                transferItems();
            }

            // move items
            if (firstItem != null) {
                moveItems();
            }
        }

        private void selectInput() {
            int[] prio = firstBlock.inputPriorities();

            ConveyorSection best = null;
            int index = -1;
            for (int i = 0; i < parents.length; i++) {
                ConveyorSection cur = parents[i];
                if (cur != null) {
                    cur.selectedInput = false;

                    if (cur.firstItem != null
                        && (best == null
                            || cur.firstItem.remaining < best.firstItem.remaining
                            || cur.firstItem.remaining == best.firstItem.remaining && prio[i] > prio[index])) {
                        best = cur;
                        index = i;
                    }
                }
            }

            if (best != null) {
                best.selectedInput = true;
            }
        }

        private void moveItems() {
            if (selectedInput && firstItem != firstNonBlockedItem && firstItem.remaining >= 8) {
                firstNonBlockedItem = firstItem;
            }

            if (firstNonBlockedItem == null) {
                return;
            }

            float spacing;
            if (firstNonBlockedItem == firstItem && (childrenCount == 0 || selectedInput)) {
                spacing = 0;
            } else {
                spacing = itemSpacing;
            }

            firstNonBlockedItem.remaining -= conveyorSpeed();
            if (firstNonBlockedItem.remaining < spacing) {
                firstNonBlockedItem.remaining = spacing;

                do {
                    firstNonBlockedItem = firstNonBlockedItem.previous;
                } while (firstNonBlockedItem != null && firstNonBlockedItem.remaining == spacing);
            } else {
                availableLength += conveyorSpeed();
            }
        }

        // **********************
        // * item pass/transfer *
        // **********************

        /**
         * Transfer all items where remaining is strictly less than conveyor speed
         */
        private void transferItems() {
            boolean transferred = true;
            while (firstItem != null && firstItem.remaining < conveyorSpeed() && transferred) {
                transferred = false;

                for (int attempt = 0; attempt < 3 && !transferred; attempt++) {
                    RelativeDirection output = endBlock.outputPriority(firstItem.item, attempt);

                    ConveyorSection sub = children[output.ordinal()];
                    if (sub != null) {
                        float remaining = firstItem.remaining;
                        if (sub.acceptItem(firstItem.item)) {
                            Item item = removeFirstItem();
                            sub.passItem(item, conveyorSpeed() - remaining);
                            sub.firstBlock.updateInputPriority(relative(sub.firstBlock.direction, output.absolute(endBlock.direction).opposite()));
                            transferred = true;
                        }
                    } else {
                        Tile next = endBlock.adjacentRelative(output);
                        Block block = next.getBlock();

                        if (block instanceof ItemConsumer) {
                            transferred = transferFirstItemTo(next, (ItemConsumer) block);
                        }
                    }
                }
            }
        }

        private boolean transferFirstItemTo(Tile tile, ItemConsumer consumer) {
            if (consumer.acceptItem(tile, firstItem.item)) {
                Item item = removeFirstItem();
                consumer.passItem(tile, item);
                return true;
            }
            return false;
        }

        private Item removeFirstItem() {
            if (firstItem == null) {
                return null;
            }

            ItemList prev = firstItem.previous;
            if (prev != null) {
                prev.next = null;
            } else {
                lastItem = null;
            }

            Item item = firstItem.item;
            itemListPool.free(firstItem);

            firstItem = prev;
            firstNonBlockedItem = prev;

            return item;
        }

        public void passItem(Item item) {
            passItem(item, 0);
        }

        private void passItem(Item item, float advance) {
            if (!acceptItem(item)) {
                return;
            }

            ItemList list = itemListPool.obtain();
            list.item = item;
            list.next = lastItem;
            list.previous = null;

            if (lastItem == null) { // no item on conveyor
                // update distances
                list.remaining = length - Math.min(advance, length);
                availableLength = length - list.remaining;

                // update linked list
                firstNonBlockedItem = list.remaining == 0 ? null : list;
                firstItem = list;
                lastItem = list;
            } else {
                // update distances
                float adv = Math.min(advance, availableLength - itemSpacing);

                list.remaining = availableLength - adv;
                availableLength = adv;

                // update linked list
                lastItem.previous = list;
                lastItem = list;

                if (firstNonBlockedItem == null && list.remaining > itemSpacing) {
                    firstNonBlockedItem = list;
                }
            }
        }

        public boolean acceptItem(Item item) {
            return availableLength >= itemSpacing;
        }


        // *******************
        // * topology update *
        // *******************


        public void appendBefore(ConveyorData before) {
            firstBlock.previousBlock = before;
            before.section = this;
            firstBlock = before;

            length += TILE_SIZE;
            availableLength += TILE_SIZE;
        }

        public void appendAfter(ConveyorData after) {
            after.previousBlock = endBlock;
            after.section = this;
            endBlock = after;

            length += TILE_SIZE;

            if (firstItem != null) {
                firstItem.remaining += TILE_SIZE;
                firstNonBlockedItem = firstItem;
            } else {
                availableLength += TILE_SIZE;
            }
        }

        /**
         * block will be in this section
         */
        public void splitAt(ConveyorData block) {
            if (block == firstBlock) {
                return;
            }

            ConveyorSection parent = new ConveyorSection();
            // update first/end tiles
            parent.firstBlock = firstBlock;
            parent.endBlock = block.previousBlock;
            firstBlock = block;

            // update parents
            // copy parents of this to parent
            copyParentsTo(this, parent);
            addParent(parent);

            // move items and update lengths
            BlockIterator it = BlockIterator.instance;
            it.set(this);
            it.moveTo(parent.endBlock);

            parent.length = length - it.totalLength;
            length = it.totalLength;

            if (it.list == null) {
                // no items move to parent
                availableLength -= parent.length;
                parent.availableLength = parent.length;

            } else if (it.list.next == null) {
                // all items move to parent

                parent.firstItem = firstItem;
                parent.lastItem = lastItem;
                parent.firstNonBlockedItem = firstItem;
                parent.availableLength = availableLength;
                firstItem.remaining -= length;

                availableLength = it.totalLength;
                firstItem = null;
                firstNonBlockedItem = null;
                lastItem = null;
            } else {
                // partial
                parent.firstItem = it.list;
                parent.lastItem = lastItem;
                parent.firstNonBlockedItem = parent.firstItem;
                parent.availableLength = availableLength;

                firstNonBlockedItem = firstItem;
                lastItem = parent.firstItem.next;
                availableLength = parent.firstItem.remaining - it.length;

                parent.firstItem.remaining = it.length;
                parent.firstItem.next = null;
                lastItem.previous = null;
            }


            // update block data
            block.previousBlock = null;

            ConveyorData b = parent.endBlock;
            while (b != null) {
                b.section = parent;
                b = b.previousBlock;
            }

            sections.add(parent);
        }

        public void addParent(ConveyorSection section) {
            if (section.endBlock.adjacentRelative(FACING) != firstBlock.tile) {
                throw new IllegalArgumentException("'section' cannot be parent because the end block isn't facing to the section");
            }

            RelativeDirection r = relativePos(firstBlock, section.endBlock);
            int i = r.ordinal() - 1;

            RelativeDirection rc = relativePos(section.endBlock, firstBlock);
            int ic = rc.ordinal();

            if (parents[i] == section) {
                return;
            }
            if (parents[i] != null) {
                throw new IllegalStateException("Already has a parent in this direction: " + r);
            }
            if (section.children[ic] != null) {
                throw new IllegalStateException("'section' already has a child in direction: " + rc);
            }

            section.children[ic] = this;
            section.childrenCount++;
            parents[i] = section;
            parentCount++;
        }

        public void removeParent(ConveyorSection section) {
            RelativeDirection r = relativePos(firstBlock, section.endBlock);
            int i = r.ordinal() - 1;

            RelativeDirection rc = relativePos(section.endBlock, firstBlock);
            int ic = rc.ordinal();

            if (parents[i] != null) {
                throw new IllegalStateException("No parent found in direction " + r);
            }
            if (parents[i] != section) {
                throw new IllegalStateException("Not the same section");
            }
            section.children[ic] = null;
            section.childrenCount--;
            parents[i] = null;
            parentCount--;
        }

        public void addChild(ConveyorSection section) {
            section.addParent(this);
        }

        public void removeChild(ConveyorSection child) {
            child.removeParent(this);
        }

        public boolean isSectionStart(BlockData data) {
            return firstBlock == data;
        }

        public boolean isSectionEnd(ConveyorData data) {
            return endBlock == data;
        }

        private float conveyorSpeed() {
            return firstBlock.speed();
        }
    }

    public static class ItemList {

        public Item item;
        public ItemList previous;
        public ItemList next;
        public float remaining; // length between this item and the next one
    }

    public static class BlockIterator {

        private static final BlockIterator instance = new BlockIterator();

        private ConveyorData block;
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
            block = section.endBlock;

            totalLength = 0;
            length = list == null ? -1 : list.remaining;

            start = true;
        }

        public void next() {
            if (!hasNext() || start) {
                start = false;
                return;
            }

            block = block.previousBlock;
            totalLength += TILE_SIZE;

            if (list != null) {
                length -= TILE_SIZE;

                while (length < 0) {
                    list = list.previous;
                    if (list == null) {
                        break;
                    }
                    length += list.remaining;
                }

                if (list == null) {
                    length = -1;
                }
            }
        }

        public boolean hasNext() {
            return start || block.previousBlock != null;
        }

        public void moveTo(BlockData block) {
            while (hasNext() && this.block != block) {
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

        private ConveyorData block;
        private ItemList list;

        // distance between the current tile and the current item
        public float length;
        // TILE_SIZE * (iterated tile count - 1)
        // doesn't include current tile
        public float totalLength;

        public void set(ConveyorSection section) {
            list = initList;
            initList.previous = section.firstItem;
            block = section.endBlock;

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
                block = block.previousBlock;
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
