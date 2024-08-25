package fr.poulpogaz.run.factory.recipes;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import fr.poulpogaz.run.Content;
import fr.poulpogaz.run.ContentType;
import fr.poulpogaz.run.factory.item.Item;

public class Recipe extends Content {

    public ItemCount[] inputs;
    public ItemCount[] outputs;
    public int duration;
    public TextureRegion icon;

    public Recipe(String name) {
        super(name);
    }

    @Override
    public void load() {

    }

    @Override
    public ContentType getContentType() {
        return ContentType.RECIPE;
    }

    public static class ItemCount {
        public final Item item;
        public final int count;

        public ItemCount(Item item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public static class Builder {
        private final Array<ItemCount> inputs = new Array<>();
        private final Array<ItemCount> outputs = new Array<>();
        private int duration;
        private TextureRegion icon;

        public Builder() {

        }

        public Recipe build(String name) {
            Recipe r = new Recipe(name);
            r.duration = duration;
            r.icon = icon;
            r.inputs = inputs.toArray(ItemCount.class);
            r.outputs = outputs.toArray(ItemCount.class);

            return r;
        }

        public Builder input(Item item, int count) {
            inputs.add(new ItemCount(item, count));
            return this;
        }

        public Builder output(Item item, int count) {
            outputs.add(new ItemCount(item, count));
            return this;
        }

        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder icon(TextureRegion icon) {
            this.icon = icon;
            return this;
        }
    }
}
