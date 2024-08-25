package fr.poulpogaz.run.factory.item;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Content;
import fr.poulpogaz.run.ContentType;
import fr.poulpogaz.run.factory.Icon;

import static fr.poulpogaz.run.Variables.atlas;

public class Item extends Content implements Icon {

    private TextureRegion icon;
    public boolean canBeGenerated;
    public int value;

    public Item(String name) {
        super(name);
        Items.all.add(this);
    }

    @Override
    public void load() {
        icon = atlas.findRegion(name);
    }

    public boolean canBeGenerated() {
        return canBeGenerated;
    }

    @Override
    public TextureRegion getIcon() {
        return icon;
    }

    public int value() {
        return value;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.ITEM;
    }
}
