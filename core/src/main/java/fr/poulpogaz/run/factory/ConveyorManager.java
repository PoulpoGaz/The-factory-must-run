package fr.poulpogaz.run.factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
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

    private static final Connection[] connections = new Connection[4];

    private static final Array<ConveyorSection> sections = new Array<>();
    private static int updatePass = 0;

    public static void newConveyor(Tile conveyor) {
        IConveyorBlock block = (IConveyorBlock) conveyor.getBlock();
        ConveyorData data = (ConveyorData) conveyor.getBlockData();

        block.connections(data, connections);
        int input = count(connections, Connection.INPUT);
        int output = count(connections, Connection.OUTPUT);

        if (input >= 2 || output >= 2) {
            // first,process inputs: grow inputs
            for (int i = 0; i < 4; i++) {
                if (connections[i] != Connection.INPUT) {
                    continue;
                }

                ConveyorSection sec = adjacentSection(data, values[i]);
                sec.grow(HALF_TILE_SIZE);
            }

            // then process outputs, grow outputs and addParent
            for (int i = 0; i < 4; i++) {
                if (connections[i] != Connection.OUTPUT && !block.forceOutput(values[i])) {
                    continue;
                }

                ConveyorSection sec = adjacentSection(data, values[i]);

                if (sec == null) {
                    sec = new ConveyorSection(conveyor, true, true);
                    sections.add(sec);
                } else {
                    sec.appendBeforeFirst(data, HALF_TILE_SIZE);
                }

                for (int j = 0; j < 4; j++) {
                    if (connections[j] == Connection.INPUT) {
                        sec.addParent(adjacentSection(data, values[j]), false);
                    }
                }
            }
        } else {
            // new section
            // or grow one section, eventually split facing and/or merge with behind
            RelativeDirection inDir = indexOfAsDirection(connections, Connection.INPUT);
            RelativeDirection outDir = indexOfAsDirection(connections, Connection.OUTPUT);

            if (outDir != null) {
                linkWithFacing(conveyor);

                if (inDir != null) {
                    ConveyorData behind = (ConveyorData) data.adjacentRelative(inDir).getBlockData();

                    merge(behind.conveyorSection(inDir.behind()),
                          data.conveyorSection(inDir));
                }
            } else if (inDir != null) {
                linkWithBehind(conveyor, inDir.absolute(data.direction));
            } else {
                ConveyorSection s = block.createNewSection(conveyor);
                sections.add(s);

                // router support
                for (ConveyorSection child : s.children) {
                    if (child != null) {
                        sections.add(child);
                    }
                }
            }
        }
    }


    private static <T> int count(T[] array, T element) {
        int n = 0;
        for (T t : array) {
            if (t == element) {
                n++;
            }
        }
        return n;
    }

    private static RelativeDirection indexOfAsDirection(Connection[] array, Connection element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == element) {
                return values[i];
            }
        }

        return null;
    }

    private static ConveyorSection adjacentSection(BlockData data, RelativeDirection dir) {
        if (data == null) {
            return null;
        }
        ConveyorData adj = (ConveyorData) data.adjacentRelative(dir).getBlockData();
        if (adj == null) {
            return null;
        }

        return adj.conveyorSection(dir.absolute(data.direction).opposite().relativeTo(adj.direction));
    }


    private static void linkWithFacing(Tile conveyor) {
        Tile facing = conveyor.adjacent(conveyor.getBlockData().direction);
        ConveyorData facingData = (ConveyorData) facing.getBlockData();

        ConveyorSection facingSection = facingData.conveyorSection(BEHIND);
        boolean start = facingSection.isSectionStart(facingData);

        if (start && facingSection.parentCount == 0) {
            facingSection.appendBeforeFirst((ConveyorData) conveyor.getBlockData(), TILE_SIZE);
        } else {
            if (!start) {
                // split
                Gdx.app.debug("Conveyor", "Splitting section at tile " + facing.x + ", " + facing.y);
                facingSection.splitAt(facingData);
            }

            // create new conveyor section
            ConveyorSection s = new ConveyorSection(conveyor);
            sections.add(s);
            facingSection.addParent(s, true);
        }
    }

    private static void linkWithBehind(Tile conveyor, Direction behindDir) {
        Tile behind = conveyor.adjacent(behindDir);
        ConveyorData data = (ConveyorData) behind.getBlockData();

        data.conveyorSection(relative(data.direction, behindDir.opposite()))
            .appendAfterEnd((ConveyorData) conveyor.getBlockData());
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

        if (after != before) {
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
            before.graph.blockIterator((d) -> d.setConveyorSection(FACING, after));

            before.graph.merge(after.graph);
            after.graph = before.graph;

            sections.removeValue(before, true);
        }
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

                    batch.draw(item.item.getIcon(), itemX - HALF_TILE_SIZE / 2f, itemY - HALF_TILE_SIZE / 2f);

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

        public float length;
        public float availableLength;

        public boolean selectedInput = false;

        public boolean beingUpdated = false;

        public ConveyorSection() {
        }

        // create a section with one conveyor
        public ConveyorSection(Tile conveyor) {
            this(conveyor, false, false);
        }

        public ConveyorSection(Tile conveyor, boolean reduce, boolean offset) {
            firstBlock = (ConveyorData) conveyor.getBlockData();
            endBlock = firstBlock;
            firstBlock.setConveyorSection(FACING, this);
            graph = new BlockGraph(conveyor);

            if (reduce) {
                length = HALF_TILE_SIZE;
                availableLength = HALF_TILE_SIZE;
                graph.start.length = HALF_TILE_SIZE;

                if (offset) {
                    graph.x += HALF_TILE_SIZE * graph.start.direction.dx;
                    graph.y += HALF_TILE_SIZE * graph.start.direction.dy;
                }
            } else {
                length = TILE_SIZE;
                availableLength = TILE_SIZE;
            }
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
                spacing = itemSpacing;
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
                        if (sub.acceptItem(firstItem.item)) {
                            Item item = removeFirstItem();
                            sub.passItem(item, conveyorSpeed() - remaining);
                            sub.firstBlock.updateInputPriority(relative(sub.firstBlock.direction, output.absolute(endBlock.direction).opposite()));
                            transferred = true;
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


        public void appendBeforeFirst(ConveyorData first, int length) {
            first.setConveyorSection(FACING, this);
            firstBlock = first;

            this.length += length;
            availableLength += length;

            graph.addFirst(length, first.direction);
        }

        public void appendAfterEnd(ConveyorData after) {
            after.setConveyorSection(FACING, this);
            endBlock = after;

            length += TILE_SIZE;

            if (firstItem != null) {
                firstItem.remaining += TILE_SIZE;
                firstNonBlockedItem = firstItem;
            } else {
                availableLength += TILE_SIZE;
            }

            graph.addLast(TILE_SIZE, after.direction);
        }


        public void grow(int length) {
            this.length += length;

            if (firstItem != null) {
                firstItem.remaining += length;
                firstNonBlockedItem = firstItem;
            } else {
                availableLength += length;
            }

            graph.end.length += length;
        }

        /**
         * block will be in this section
         */
        public void splitAt(ConveyorData block) {
            if (block == firstBlock) {
                return;
            }

            BlockFinder f = BlockFinder.instance;
            boolean found = f.find(graph, block);
            if (!found) {
                throw new IllegalStateException();
            }

            ConveyorSection parent = new ConveyorSection();
            // update first/end tiles
            parent.firstBlock = firstBlock;
            parent.endBlock = (ConveyorData) f.previousBlock();
            firstBlock = block;


            // update parents
            // copy parents of this to parent
            copyParentsTo(this, parent);
            addParent(parent, false);

            // move items and update lengths
            float cutPos = f.totalLength + HALF_TILE_SIZE;
            parent.length = cutPos;
            length = length - cutPos;

            if (firstItem == null || availableLength >= parent.length) {
                // no items move to parent
                availableLength -= parent.length;
                parent.availableLength = parent.length;
            } else if (firstItem.remaining >= length) {
                // all items move to parent
                parent.firstItem = firstItem;
                parent.lastItem = lastItem;
                parent.firstNonBlockedItem = firstItem;
                parent.availableLength = availableLength;
                firstItem.remaining -= length;

                availableLength = length;
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
                availableLength = ItemFinder.totalLength - parent.length;

                parent.firstItem.remaining -= availableLength;
                parent.firstItem.next = null;
                lastItem.previous = null;
            }

            // split block graph
            BlockGraph bis = graph.split(block);
            parent.graph = graph;
            graph = bis;

            parent.graph.blockIterator(d -> {
                if (d != firstBlock) {
                    d.setConveyorSection(FACING, parent);
                }
            });

            sections.add(parent);
        }

        public void addParent(ConveyorSection section, boolean updateGraph) {
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

            if (updateGraph) {
                section.length += HALF_TILE_SIZE;
                section.graph.end.length += HALF_TILE_SIZE;
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

        public boolean isSectionStart(BlockData data) {
            return firstBlock == data;
        }

        public boolean isSectionEnd(ConveyorData data) {
            return endBlock == data;
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

        public BlockGraph() {

        }

        public BlockGraph(Tile tile) {
            start = new BlockNode();
            start.length = TILE_SIZE;
            start.direction = tile.getBlockData().direction;
            end = start;

            temp.set(-HALF_TILE_SIZE, 0);
            start.direction.rotate(temp);
            x = (int) (temp.x + tile.drawX() + HALF_TILE_SIZE);
            y = (int) (temp.y + tile.drawY() + HALF_TILE_SIZE);
        }

        public void addLast(int len, Direction dir) {
            if (end.direction != dir) {
                end.length += HALF_TILE_SIZE;
                BlockNode newNode = new BlockNode();
                newNode.length = len - HALF_TILE_SIZE;
                newNode.direction = dir;

                end.next = newNode;
                newNode.previous = end;
                end = newNode;
            } else {
                end.length += len;
            }
        }

        public void addFirst(int len, Direction dir) {
            if (start.direction != dir) {
                BlockNode newNode = new BlockNode();
                newNode.length = len + HALF_TILE_SIZE;
                newNode.next = start;
                newNode.direction = dir;

                x = x + HALF_TILE_SIZE * start.direction.dx - newNode.length * dir.dx;
                y = y + HALF_TILE_SIZE * start.direction.dy - newNode.length * dir.dy;

                start.previous = newNode;
                start.length -= HALF_TILE_SIZE;
                start = newNode;

            } else {
                start.length += len;
                x -= dir.dx * len;
                y -= dir.dy * len;
            }
        }

        public BlockGraph split(BlockData block) {
            BlockFinder f = BlockFinder.instance;

            if (f.find(this, block)) {
                BlockNode node = f.node;

                BlockGraph g = new BlockGraph();
                g.x = f.x + HALF_TILE_SIZE * node.direction.dx;
                g.y = f.y + HALF_TILE_SIZE * node.direction.dy;

                if (node.length - f.length <= HALF_TILE_SIZE) {
                    g.start = node.next;
                    g.start.previous = null;
                    g.end = end;

                    end = node;
                    end.next = null;
                } else {
                    BlockNode newNode = new BlockNode();
                    newNode.direction = node.direction;
                    newNode.length = node.length - f.length - HALF_TILE_SIZE;
                    node.length = f.length + HALF_TILE_SIZE;

                    if (node == end) {
                        g.start = newNode;
                        g.end = newNode;
                    } else {
                        g.start = newNode;
                        g.end = end;

                        newNode.next = node.next;
                        node.next.previous = newNode;
                        node.next = null;

                        end = node;
                    }
                }

                return g;
            }

            return null;
        }

        public void merge(BlockGraph after) {
            if (end.direction != after.start.direction) {
                end.length += HALF_TILE_SIZE;
                after.start.length -= HALF_TILE_SIZE;

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
        }

        public void blockIterator(Consumer<ConveyorData> consumer) {
            int x = this.x;
            int y = this.y;

            BlockNode node = this.start;
            ConveyorData last = null;
            while (node != null) {
                int len = node.length;

                while (true) {
                    BlockData n = factory.getBlockData(x / TILE_SIZE, y / TILE_SIZE);
                    if (last != n && n instanceof ConveyorData) { // TODO: check first block
                        ConveyorData next = (ConveyorData) n;
                        consumer.accept(next);
                        last = next;
                    }

                    if (len == 0) {
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
    }

    public static class BlockNode {

        public int length;
        public Direction direction;
        public BlockNode next;
        public BlockNode previous;

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
