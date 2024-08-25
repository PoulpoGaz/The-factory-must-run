package fr.poulpogaz.run.factory.recipes;

import fr.poulpogaz.run.factory.item.Items;

import static fr.poulpogaz.run.Variables.atlas;

public class Recipes {

    public static Recipe NOTHING;
    public static Recipe GEAR;

    public static void load() {
        NOTHING = new Recipe.Builder()
            .icon(atlas.findRegion("nothing")).build("nothing");
        GEAR = new Recipe.Builder()
            .input(Items.IRON_PLATE, 1)
            .output(Items.GEAR, 2)
            .duration(60)
            .icon(atlas.findRegion("gear")).build("gear");
    }
}
