package fr.poulpogaz.run.factory;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import fr.poulpogaz.run.Direction;
import fr.poulpogaz.run.RelativeDirection;
import fr.poulpogaz.run.Utils;
import fr.poulpogaz.run.factory.blocks.*;
import fr.poulpogaz.run.factory.blocks.IConveyorBlock.Connection;
import fr.poulpogaz.run.factory.item.Item;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import static fr.poulpogaz.run.RelativeDirection.*;
import static fr.poulpogaz.run.Variables.*;

public class ConveyorManager {

    private static final Pool<ItemList> itemListPool = Utils.newPool(ItemList::new);

    private static final Array<ConveyorSection> sections = new Array<>();
    private static int updatePass = 0;

    /**
     * @return the section just after 'data' in direction 'dir'
     */
    public static ConveyorSection adjacentSection(BlockData data, RelativeDirection dir) {
        if (data != null) {
            Tile tile = data.adjacentRelative(dir);
            if (tile != null && tile.getBlock().isConveyor()) {
                ConveyorData adj = (ConveyorData) tile.getBlockData();
                if (adj != null) {
                    return adj.conveyorSection(dir.absolute(data.direction).opposite().relativeTo(adj.direction));
                }
            }
        }

        return null;
    }

    /**
     * Set up an output in direction 'dir':
     * if connections[dir] == OUTPUT, link with conveyor section in direction 'dir'
     *    and grow the section backward by HALF_TILE_SIZE
     * else create a new section of size HALF_TILE_SIZE centered at data position
     *
     * @return the section in the direction 'dir'
     */
    public static ConveyorSection setupOutput(ConveyorData data,
                                              Connection[] connections,
                                              RelativeDirection dir) {
        // update facing block if needed
        if (connections[dir.ordinal()] == Connection.OUTPUT) {
            Tile adj = data.adjacentRelative(dir);
            ConveyorData facingData = (ConveyorData) adj.getBlockData();
            IConveyorBlock facing = (IConveyorBlock) adj.getBlock();
            facing.newInput((ConveyorData) adj.getBlockData(), relativePos(facingData, data));
        }

        // create section if needed or grow
        ConveyorManager.ConveyorSection s = ConveyorManager.adjacentSection(data, dir);
        if (s == null || connections[dir.ordinal()] != Connection.OUTPUT) {
            s = new ConveyorManager.ConveyorSection()
                .init(data, dir, false);
            data.setConveyorSection(dir, s);
        } else {
            s.growFirst(data, HALF_TILE_SIZE);
        }

        return s;
    }

    /**
     * Merge 'before' and 'after'.
     * 'before' will be removed
     */
    public static void merge(ConveyorSection before, ConveyorSection after) {
        // if (before.endBlock != after.endBlock) {
        //     throw new IllegalStateException("Cannot merge: disconnected sections");
        // }

        if (after != before) {
            after.firstBlock = before.firstBlock;
            // update parents
            moveParentsTo(before, after);

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
                after.firstItem.remaining += after.graph.length;
                after.availableLength = before.availableLength;
            } else {
                // no item to move
                after.availableLength += before.availableLength;
            }

            // update block data
            before.blockIterator((d) -> {
                d.setConveyorSection(FACING, after);
            });
            after.firstBlock.setConveyorSection(FACING, after); // underground conveyor hack

            before.graph.merge(after.graph);
            after.graph = before.graph;

            before.removeSection();
        }
    }

    private static void moveParentsTo(ConveyorSection src, ConveyorSection dest) {
        dest.parentCount = src.parentCount;
        System.arraycopy(src.parents, 0, dest.parents, 0, 3);
        for (int i = 0; i < 3; i++) {
            ConveyorSection parPar = dest.parents[i];
            if (parPar != null) {
                for (int ic = 0; ic < 3; ic++) {
                    if (parPar.children[ic] == src) {
                        parPar.children[ic] = dest;
                        break;
                    }
                }
            }
        }

        Arrays.fill(src.parents, null);
        src.parentCount = 0;
    }

    public static void tick() {
        updatePass++;

        for (int i = 0; i < sections.size; i++) {
            sections.get(i).update();
        }
    }

    public static void drawItems() {
        for (int i = 0; i < sections.size; i++) {
            ConveyorSection section = sections.get(i);
            if (section.firstItem == null) {
                continue;
            }

            BlockGraph graph = section.graph;
            BlockNode node = graph.start;

            int x = graph.x;
            int y = graph.y;

            ItemList item = section.lastItem;
            float length = section.availableLength;

            while (node != null) {
                while (item != null && length <= node.length) {
                    float itemX = x + length * node.direction.dx;
                    float itemY = y + length * node.direction.dy;

                    if (!node.hidden) {
                        batch.draw(item.item.getIcon(), itemX - HALF_TILE_SIZE / 2f, itemY - HALF_TILE_SIZE / 2f);
                    } else if (debug) {
                        batch.end();
                        shape.begin(ShapeRenderer.ShapeType.Line);
                        shape.setColor(0, 1, 0, 1);
                        shape.rect(itemX - HALF_TILE_SIZE / 2f, itemY - HALF_TILE_SIZE / 2f, HALF_TILE_SIZE, HALF_TILE_SIZE);
                        shape.end();
                        batch.begin();
                    }

                    length += item.remaining;
                    item = item.next;
                }

                length -= node.length;
                x += node.dx();
                y += node.dy();
                node = node.next;
            }
        }
    }

    public static void drawGraphs() {
        batch.end();

        int tx = input.tileX;
        int ty = input.tileY;
        BlockData data = null;
        if (factory.isInFactory(tx, ty)) {
            data = factory.getBlockData(tx, ty);
        }

        for (int i = 0; i < sections.size; i++) {
            ConveyorSection section = sections.get(i);
            BlockGraph graph = section.graph;

            if (graph == null) {
                continue;
            }

            batch.begin();
            font.setColor(Color.WHITE);
            font.draw(batch, Integer.toString(i), section.firstBlock.drawX(), section.firstBlock.drawY() + TILE_SIZE);
            batch.end();
            shape.begin(ShapeRenderer.ShapeType.Filled);

            BlockFinder f = BlockFinder.instance;
            boolean found = false;
            if (data != null) {
                found = f.find(graph, data);
            }

            float x = graph.x;
            float y = graph.y;

            BlockNode node = graph.start;
            while (node != null) {
                float r = node == graph.start || node == graph.end ? 3 : 2;
                shape.getColor().fromHsv(360f * i / sections.size, 1, 1);
                shape.circle(x, y, r);

                boolean draw = found && node == f.node;

                if (draw) {
                    shape.setColor(Color.WHITE);
                }
                shape.line(x, y, x + node.dx(), y + node.dy());

                if (draw) {
                    shape.setColor(Color.BLACK);
                    shape.line(x, y, x + node.direction.dx * f.length, y + node.direction.dy * f.length);
                }

                x = x + node.dx();
                y = y + node.dy();
                node = node.next;
            }

            shape.end();
        }

        batch.begin();

        /*for (int y = 0; y < factory.getHeight(); y++) {
            for (int x = 0; x < factory.getWidth(); x++) {
                BlockData d = factory.getBlockData(x, y);

                if (d instanceof ConveyorData) {
                    ConveyorData conv = (ConveyorData) d;
                    if (conv.section != null) {
                        int i = ConveyorManager.sectionIndex(conv.section);

                        font.setColor(Color.WHITE);
                        Utils.drawStringCentered(Integer.toString(i), conv.drawX(), conv.drawY() + TILE_SIZE, TILE_SIZE,
                                                 TILE_SIZE);
                    }
                }
            }
        }*/
    }

    private static int sectionIndex(ConveyorSection s) {
        return sections.indexOf(s, true);
    }

    public static class ConveyorSection {

        public static final int itemSpacing = 8;

        // parents[0] -> left
        // parents[1] -> right
        // parents[2] -> behind
        // => RelativeDirection.ordinal() - 1
        public final ConveyorSection[] parents = new ConveyorSection[3];
        public int parentCount;
        // children[0] -> facing
        // children[1] -> left
        // children[2] -> right
        // => RelativeDirection.ordinal()
        public final ConveyorSection[] children = new ConveyorSection[3];
        public int childrenCount;

        public ConveyorData firstBlock; // block in which items are added
        public ConveyorData endBlock;

        public BlockGraph graph;

        public int lastUpdate;

        public ItemList firstItem;
        public ItemList firstNonBlockedItem;
        public ItemList lastItem;

        public float availableLength;

        public boolean selectedInput = false;

        public boolean beingUpdated = false;

        public ConveyorSection() {
            sections.add(this);
        }

        public ConveyorSection init(ConveyorData data, RelativeDirection dir, boolean input) {
            firstBlock = data;
            endBlock = data;
            graph = new BlockGraph();

            Direction abs = dir.absolute(data.direction);
            if (input) {
                graph.init(data.tile, abs, HALF_TILE_SIZE, abs.opposite());
            } else {
                graph.init(data.tile, null, HALF_TILE_SIZE, abs);
            }
            availableLength = graph.length;
            return this;
        }

        public void update() {
            if (lastUpdate == updatePass || beingUpdated) {
                return;
            }

            beingUpdated = true;
            for (ConveyorSection s : children) {
                if (s != null) {
                    s.update();
                }
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

            beingUpdated = false;
        }

        private void selectInput() {
            ConveyorSection best = null;
            int bestPrio = Integer.MAX_VALUE;
            for (int i = 0; i < parents.length; i++) {
                ConveyorSection cur = parents[i];
                if (cur != null) {
                    cur.selectedInput = false;

                    if (cur.firstItem != null) {
                        int prio = firstBlock.inputPriority(cur.firstItem.item, values[i + 1]);

                        if (best == null
                                || cur.firstItem.remaining < best.firstItem.remaining
                                || cur.firstItem.remaining == best.firstItem.remaining && prio < bestPrio) {
                            best = cur;
                            bestPrio = prio;
                        }
                    }
                }
            }

            if (best != null) {
                best.selectedInput = true;
            }
        }

        private void moveItems() {
            ConveyorSection sub = children[FACING.ordinal()];

            if (selectedInput
                && firstItem != firstNonBlockedItem
                && firstItem.remaining + sub.availableLength > itemSpacing) {
                firstNonBlockedItem = firstItem;
            }

            if (firstNonBlockedItem == null) {
                return;
            }

            float spacing;
            if (firstNonBlockedItem == firstItem && childrenCount == 0) {
                spacing = 0;
            } else if (firstNonBlockedItem == firstItem && selectedInput) {
                spacing = Math.max(itemSpacing - sub.availableLength, 0);
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
                        if (sub.availableLength > itemSpacing) {
                            Item item = removeFirstItem();
                            sub.firstBlock.updateInputPriority(lastDirection().opposite().relativeTo(sub.firstDirection()));
                            sub.passItem(item, conveyorSpeed() - remaining);
                            transferred = true;

                            for (ConveyorSection child : children) {
                                if (child != null) {
                                    child.selectInput();
                                }
                            }
                        }
                    } else {
                        Tile next = endBlock.adjacentRelative(output);
                        Block block = next.getBlock();

                        if (block instanceof ItemConsumer && !(block instanceof IConveyorBlock)) {
                            transferred = transferFirstItemTo(next, (ItemConsumer) block);
                        }
                    }

                    if (transferred) {
                        endBlock.updateOutputPriority(attempt);
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

        private Item removeLastItem() {
            if (lastItem == null) {
                return null;
            }

            ItemList next = lastItem.next;
            if (next != null) {
                next.previous = null;
            } else {
                firstItem = null;
            }

            Item item = lastItem.item;
            itemListPool.free(lastItem);

            lastItem = next;

            return item;
        }

        public boolean passItem(Item item) {
            return passItem(item, 0);
        }

        private boolean passItem(Item item, float advance) {
            if (!acceptItem(item)) {
                return false;
            }

            ItemList list = itemListPool.obtain();
            list.item = item;
            list.next = lastItem;
            list.previous = null;

            if (lastItem == null) { // no item on conveyor
                // update distances
                list.remaining = graph.length - Math.min(advance, graph.length);
                availableLength = graph.length - list.remaining;

                // update linked list
                firstNonBlockedItem = list.remaining == 0 ? null : list;
                firstItem = list;
                lastItem = list;

                for (ConveyorSection child : children) {
                    if (child != null) {
                        child.selectInput();
                    }
                }
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

            return true;
        }

        public boolean acceptItem(Item item) {
            return availableLength >= itemSpacing;
        }


        // *******************
        // * topology update *
        // *******************


        public void appendBeforeFirst(ConveyorData first, int length) {
            appendBeforeFirst(first, length, false);
        }

        public void appendBeforeFirst(ConveyorData first, int length, boolean hidden) {
            first.setConveyorSection(FACING, this);
            firstBlock = first;
            availableLength += length;

            graph.addFirst(length, first.direction, hidden);
        }

        public void growFirst(ConveyorData first, int length) {
            first.setConveyorSection(FACING, this);
            firstBlock = first;
            availableLength += length;

            graph.growStart(length);
        }

        public void appendAfterEnd(ConveyorData after, int length) {
            after.setConveyorSection(FACING, this);
            endBlock = after;

            if (firstItem != null) {
                firstItem.remaining += length;
                firstNonBlockedItem = firstItem;
            } else {
                availableLength += length;
            }

            graph.addLast(length, after.direction, false);
        }

        public void appendAfterEnd(Direction direction, int length, boolean hidden) {
            if (firstItem != null) {
                firstItem.remaining += length;
                firstNonBlockedItem = firstItem;
            } else {
                availableLength += length;
            }

            graph.addLast(length, direction, hidden);
        }


        public void growEnd(ConveyorData newEnd, int length) {
            graph.growEnd(length);
            endBlock = newEnd;

            if (firstItem != null) {
                firstItem.remaining += length;
                firstNonBlockedItem = firstItem;
            } else {
                availableLength += length;
            }
        }

        public void shrinkStart(int length) {
            shrinkStart(length, true);
        }

        public void shrinkStart(int length, boolean setFirstBlock) {
            if (setFirstBlock) {
                this.firstBlock = (ConveyorData) firstBlock.adjacent(graph.start.direction)
                                                           .getBlockData();
            }
            graph.shrinkStart(length);

            if (lastItem != null && length > availableLength) {
                while (length > availableLength && lastItem != null) {
                    availableLength += lastItem.remaining;
                    removeLastItem();
                }
            }
            availableLength -= length;
            firstNonBlockedItem = firstItem;
        }

        public void shrinkEnd(int length) {
            shrinkEnd(length, true);
        }

        public void shrinkEnd(int length, boolean setEndBlock) {
            graph.shrinkEnd(length);
            if (setEndBlock) {
                endBlock = (ConveyorData) endBlock.adjacent(graph.end.direction.opposite())
                                                  .getBlockData();
            }

            if (firstItem != null) {
                float remaining = length;

                while (remaining > 0 && firstItem != null) {
                    if (remaining > firstItem.remaining) {
                        remaining -= firstItem.remaining;
                        removeFirstItem();
                    } else {
                        firstItem.remaining -= remaining;
                        remaining = 0;
                    }
                }

                if (firstItem == null) {
                    availableLength = graph.length;
                }

                firstNonBlockedItem = firstItem;
            } else {
                availableLength -= length;
            }
        }

        /**
         * block will be in this section
         */
        public ConveyorSection splitAt(ConveyorData block, boolean addParent) {
            if (block == firstBlock) {
                return null;
            }

            BlockFinder f = BlockFinder.instance;
            boolean found = f.find(graph, block);
            if (!found) {
                throw new IllegalStateException();
            }

            ConveyorSection parent = new ConveyorSection();
            // update first/end tiles
            parent.firstBlock = firstBlock;
            parent.endBlock = block;
            firstBlock = block;

            // split block graph
            BlockGraph bis = graph.split(block);
            parent.graph = graph;
            graph = bis;

            // update parents
            // copy parents of this to parent
            moveParentsTo(this, parent);
            if (addParent) {
                addParent(parent);
            }

            // move items and update lengths
            float cutPos = f.totalLength + HALF_TILE_SIZE;

            if (firstItem == null || availableLength >= parent.graph.length) {
                // no items move to parent
                availableLength -= parent.graph.length;
                parent.availableLength = parent.graph.length;
            } else if (firstItem.remaining >= graph.length) {
                // all items move to parent
                parent.firstItem = firstItem;
                parent.lastItem = lastItem;
                parent.firstNonBlockedItem = firstItem;
                parent.availableLength = availableLength;
                firstItem.remaining -= graph.length;

                availableLength = graph.length;
                firstItem = null;
                firstNonBlockedItem = null;
                lastItem = null;
            } else {
                // partial
                // first item after cutPos
                ItemList item = ItemFinder.findItemAfter(this, cutPos);
                Objects.requireNonNull(item);

                parent.lastItem = lastItem;
                lastItem = item;

                parent.firstItem = item.previous;
                parent.firstNonBlockedItem = parent.firstItem;
                parent.availableLength = availableLength;

                firstNonBlockedItem = firstItem;
                availableLength = ItemFinder.totalLength - parent.graph.length;

                parent.firstItem.remaining -= availableLength;
                parent.firstItem.next = null;
                lastItem.previous = null;
            }

            parent.blockIterator(d -> {
                if (d != parent.endBlock) {
                    d.setConveyorSection(FACING, parent);
                }
            });

            return parent;
        }

        public void addParent(ConveyorSection section) {
            if (section.endBlock != firstBlock) {
                throw new IllegalArgumentException("'section' cannot be parent because the end block isn't facing to the section");
            }

            RelativeDirection r = section.lastDirection().opposite().relativeTo(firstDirection());

            // direction of the block behind the last block of 'section
            BlockData behindData = section.endBlock.adjacent(section.lastDirection().opposite()).getBlockData();
            Direction behindDir = behindData == null ? section.lastDirection() : behindData.direction;
            RelativeDirection rc = section.lastDirection().relativeTo(behindDir);

            addParent(section, r, rc);
        }

        public void addParent(ConveyorSection section, RelativeDirection r, RelativeDirection rc) {
            if (section.endBlock != firstBlock) {
                throw new IllegalArgumentException("'section' cannot be parent because the end block isn't facing to the section");
            }

            int i = r.ordinal() - 1;
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

        public void removeParent(ConveyorSection section, RelativeDirection r, RelativeDirection rc) {
            int i = r.ordinal() - 1;
            int ic = rc.ordinal();

            if (parents[i] == null) {
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

        public void removeSection() {
            sections.removeValue(this, true);
        }


        public void blockIterator(Consumer<ConveyorData> consumer) {
            int x = graph.x;
            int y = graph.y;

            BlockNode node = graph.start;
            ConveyorData last = null;
            while (node != null) {
                int len = node.length;

                while (true) {
                    BlockData n = factory.getBlockData(x / TILE_SIZE, y / TILE_SIZE);
                    if ((last == null && n == firstBlock || last != n) && n instanceof ConveyorData) {
                        ConveyorData next = (ConveyorData) n;
                        consumer.accept(next);
                        last = next;
                    }

                    if (len == 0 || n == endBlock) {
                        break;
                    }

                    int delta = Math.min(TILE_SIZE, len);
                    len -= delta;
                    x += node.direction.dx * delta;
                    y += node.direction.dy * delta;
                }
                node = node.next;
            }
        }



        public boolean isSectionStart(BlockData data) {
            return firstBlock == data;
        }

        public boolean isSectionEnd(ConveyorData data) {
            return endBlock == data;
        }

        public Direction firstDirection() {
            return graph.start.direction;
        }

        public Direction lastDirection() {
            return graph.end.direction;
        }

        private float conveyorSpeed() {
            return firstBlock.speed();
        }

        public boolean hasParent() {
            return parentCount > 0;
        }

        public ConveyorSection getParent(RelativeDirection dir) {
            return parents[dir.ordinal() - 1];
        }

        public ConveyorSection getUniqueParent() {
            if (parentCount == 1) {
                for (ConveyorSection p : parents) {
                    if (p != null) {
                        return p;
                    }
                }
            }

            return null;
        }

        public ConveyorSection getUniqueChild() {
            if (childrenCount == 1) {
                for (ConveyorSection p : children) {
                    if (p != null) {
                        return p;
                    }
                }
            }

            return null;
        }

        public ConveyorSection getChild(RelativeDirection dir) {
            return children[dir.ordinal()];
        }
    }

    public static class ItemList {

        public Item item;
        public ItemList previous;
        public ItemList next;
        public float remaining; // length between this item and the next one
    }

    public static class BlockGraph {

        public BlockNode start;
        public BlockNode end;
        public int x;
        public int y;
        public int length;

        public BlockGraph() {
            start = new BlockNode();
            end = start;
        }

        public BlockGraph init(Tile tile, Direction offsetDir, int length, Direction direction) {
            x = tile.drawX() + HALF_TILE_SIZE;
            y = tile.drawY() + HALF_TILE_SIZE;

            if (offsetDir != null) {
                x += offsetDir.dx * HALF_TILE_SIZE;
                y += offsetDir.dy * HALF_TILE_SIZE;
            }

            start.direction = direction;
            start.length = length;
            this.length = length;

            return this;
        }

        public void addLast(int len, Direction dir, boolean hidden) {
            if (end.direction != dir && !hidden) { // quick fix for underground conveyor
                end.length += HALF_TILE_SIZE;
                BlockNode newNode = new BlockNode();
                newNode.length = len - HALF_TILE_SIZE;
                newNode.direction = dir;
                newNode.hidden = false;

                end.next = newNode;
                newNode.previous = end;
                end = newNode;
            } else if (end.hidden != hidden || end.direction != dir) { // quick fix for underground conveyor
                BlockNode newNode = new BlockNode();
                newNode.length = len;
                newNode.direction = dir;
                newNode.hidden = hidden;

                end.next = newNode;
                newNode.previous = end;
                end = newNode;
            } else {
                end.length += len;
            }

            length += len;
        }

        public void addFirst(int len, Direction dir, boolean hidden) {
            if (start.direction != dir || hidden != start.hidden) {
                BlockNode newNode = new BlockNode();
                newNode.length = len;
                newNode.next = start;
                newNode.direction = dir;
                newNode.hidden = hidden;

                start.previous = newNode;
                start = newNode;
            } else {
                start.length += len;
            }

            x -= dir.dx * len;
            y -= dir.dy * len;
            length += len;
        }

        public BlockGraph split(BlockData block) {
            BlockFinder f = BlockFinder.instance;

            if (f.find(this, block)) {
                BlockNode node = f.node;

                BlockGraph after = new BlockGraph();
                after.x = f.x + HALF_TILE_SIZE * node.direction.dx;
                after.y = f.y + HALF_TILE_SIZE * node.direction.dy;
                after.length = length - f.totalLength - HALF_TILE_SIZE;
                length = length - after.length;

                if (node.length - f.length <= HALF_TILE_SIZE) {
                    after.start = node.next;
                    after.start.previous = null;
                    after.end = end;

                    end = node;
                    end.next = null;
                } else {
                    BlockNode newNode = new BlockNode();
                    newNode.direction = node.direction;
                    newNode.length = node.length - f.length - HALF_TILE_SIZE;
                    node.length = f.length + HALF_TILE_SIZE;

                    if (node == end) {
                        after.start = newNode;
                        after.end = newNode;
                    } else {
                        after.start = newNode;
                        after.end = end;

                        newNode.next = node.next;
                        node.next.previous = newNode;
                        node.next = null;

                        end = node;
                    }
                }

                return after;
            }

            return null;
        }

        public void merge(BlockGraph after) {
            if (end.direction != after.start.direction || end.hidden != after.start.hidden) {
                end.next = after.start;
                after.start.previous = end;
                end = after.end;
            } else {
                end.length += after.start.length;
                end.next = after.start.next;

                if (end.next != null) {
                    end.next.previous = end;
                    end = after.end;
                }
            }

            length += after.length;
        }

        public void growStart(int add) {
            length += add;
            start.length += add;

            x -= start.direction.dx * add;
            y -= start.direction.dy * add;
        }

        public void growEnd(int add) {
            length += add;
            end.length += add;
        }

        public void shrinkStart(int length) {
            this.length -= length;

            while (length > 0 && start != null) {
                if (length >= start.length) {
                    length -= start.length;

                    x += start.direction.dx * start.length;
                    y += start.direction.dy * start.length;

                    start = start.next;
                    if (start != null) {
                        start.previous.next = null;
                        start.previous = null;
                    } else {
                        end = null;
                    }
                } else {
                    x += start.direction.dx * length;
                    y += start.direction.dy * length;

                    start.length -= length;
                    length = 0;
                }
            }
        }

        public void shrinkEnd(int length) {
            this.length -= length;

            while (length > 0 && end != null) {
                if (length >= end.length) {
                    length -= end.length;

                    end = end.previous;
                    if (end != null) {
                        end.next.previous = null;
                        end.next = null;
                    } else {
                        start = null;
                    }
                } else {
                    end.length -= length;
                    length = 0;
                }
            }
        }
    }

    public static class BlockNode {

        public int length;
        public Direction direction;
        public BlockNode next;
        public BlockNode previous;
        public boolean hidden;

        public int dx() {
            return length * direction.dx;
        }

        public int dy() {
            return length * direction.dy;
        }
    }

    public static class ItemFinder {

        public static float totalLength;

        /**
         * @return the first item after 'length' pixels regardless to conveyor direction
         */
        public static ItemList findItemAfter(ConveyorSection section, float length) {
            ItemList list = section.lastItem;
            if (list == null) {
                return null;
            }

            totalLength = section.availableLength;
            length -= section.availableLength;
            while (list != null && length > 0) {
                length -= list.remaining;
                totalLength += list.remaining;
                list = list.next;
            }

            return list;
        }
    }

    public static class BlockFinder {

        private static final BlockFinder instance = new BlockFinder();

        // length between the beginning of the graph and the position of the block
        public int totalLength;
        public int length; // between current node and block
        public BlockNode node;
        public int x;
        public int y;

        public BlockData block;

        public boolean find(BlockGraph graph, BlockData block) {
            this.block = block;
            totalLength = 0;
            length = 0;

            x = graph.x;
            y = graph.y;

            BlockNode node = graph.start;
            while (node != null) {
                if (node.direction.dx != 0) {
                    int x1 = (x / 32) * 32;
                    int x2 = ((x + node.dx()) / 32) * 32;
                    if (node.direction.dx < 0) {
                        x1 = x2;
                        x2 = x;
                    }

                    if (block.drawY() <= y && y <= block.drawY() + TILE_SIZE
                        && x1 <= block.drawX() && block.drawX() <= x2) {
                        length = node.direction.dx > 0 ? (block.drawX() - x) : (x - block.drawX() - TILE_SIZE);
                        totalLength += length;
                        x += node.direction.dx * length;
                        this.node = node;
                        return true;
                    }
                } else {
                    int y1 = (y / 32) * 32;
                    int y2 = ((y + node.dy()) / 32) * 32;
                    if (node.direction.dy < 0) {
                        y1 = y2;
                        y2 = y;
                    }

                    if (block.drawX() <= x && x <= block.drawX() + TILE_SIZE
                        && y1 <= block.drawY() && block.drawY() <= y2) {
                        length = node.direction.dy > 0 ? (block.drawY() - y) : (y - block.drawY() - TILE_SIZE);
                        y += node.direction.dy * length;
                        totalLength += length;
                        this.node = node;
                        return true;
                    }
                }

                x += node.dx();
                y += node.dy();
                totalLength += node.length;
                node = node.next;
            }

            return false;
        }

        public BlockData previousBlock() {
            return block.adjacent(node.direction.opposite()).getBlockData();
        }
    }
}
