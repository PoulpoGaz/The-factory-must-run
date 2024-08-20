package fr.poulpogaz.run.factory.item;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Content;
import fr.poulpogaz.run.ContentType;

import static fr.poulpogaz.run.Variables.atlas;

public class Item extends Content {

    private TextureRegion icon;

    public Item(String name) {
        super(name);
    }

    @Override
    public void load() {
        icon = atlas.findRegion(name);
    }

    public TextureRegion getIcon() {
        return icon;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.ITEM;
    }
}
