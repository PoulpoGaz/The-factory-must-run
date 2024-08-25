package fr.poulpogaz.run.factory.recipes;

import static fr.poulpogaz.run.Variables.atlas;

public class Recipes {

    public static Recipe NOTHING;

    public static void load() {
        NOTHING = new Recipe.Builder()
            .icon(atlas.findRegion("nothing")).build("nothing");
    }
}
