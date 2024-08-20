package fr.poulpogaz.run.factory;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.poulpogaz.run.Variables;

public enum Floor {

    GROUND("ground");

    private final String name;
    private final TextureRegion region;

    Floor(String name) {
        this.name = name;
        region = Variables.atlas.findRegion(name);
    }

    public String getName() {
        return name;
    }

    public TextureRegion getRegion() {
        return region;
    }
}
