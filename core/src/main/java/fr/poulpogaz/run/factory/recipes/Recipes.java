package fr.poulpogaz.run.factory.recipes;

import com.badlogic.gdx.utils.Array;
import fr.poulpogaz.run.Content;
import fr.poulpogaz.run.ContentManager;
import fr.poulpogaz.run.ContentType;
import fr.poulpogaz.run.factory.item.Items;

import static fr.poulpogaz.run.Variables.atlas;

public class Recipes {

    public static Recipe NOTHING;
    public static Recipe GEAR;
    public static Recipe PIPE;

    public static Recipe COPPER_WIRE;
    public static Recipe WOOD_PLANK;
    public static Recipe RESISTOR;
    public static Recipe INDUCTOR;
    public static Recipe CAPACITOR;
    public static Recipe TRANSISTOR;
    public static Recipe ELECTRONIC_CIRCUIT;

    public static Array<Recipe> all = new Array<>();

    public static void load() {
        NOTHING = new Recipe.Builder()
            .icon(atlas.findRegion("nothing"))
            .build("nothing");
        GEAR = new Recipe.Builder()
            .input(Items.IRON_PLATE, 1)
            .output(Items.GEAR, 2)
            .duration(60)
            .icon(atlas.findRegion("gear")).build("gear");
        PIPE = new Recipe.Builder()
            .input(Items.IRON_PLATE, 1)
            .output(Items.GEAR, 1)
            .duration(60)
            .icon(atlas.findRegion("pipe")).build("pipe");

        COPPER_WIRE = new Recipe.Builder()
            .input(Items.COPPER_PLATE, 1)
            .output(Items.COPPER_WIRE, 2)
            .duration(30)
            .icon(atlas.findRegion("copper_wire")).build("copper_wire");
        WOOD_PLANK = new Recipe.Builder()
            .input(Items.WOOD, 1)
            .output(Items.WOOD_PLANK, 1)
            .duration(90)
            .icon(atlas.findRegion("wood_plank")).build("wood_plank");
        INDUCTOR = new Recipe.Builder()
            .input(Items.WOOD_PLANK, 1)
            .input(Items.COPPER_WIRE, 2)
            .output(Items.INDUCTOR, 1)
            .duration(60)
            .icon(atlas.findRegion("inductor")).build("inductor");
        CAPACITOR = new Recipe.Builder()
            .input(Items.IRON_PLATE, 2)
            .input(Items.COPPER_WIRE, 1)
            .output(Items.CAPACITOR, 1)
            .duration(90)
            .icon(atlas.findRegion("capacitor")).build("capacitor");
        RESISTOR = new Recipe.Builder()
            .input(Items.COPPER_WIRE, 1)
            .input(Items.CARBON, 2)
            .output(Items.RESISTOR, 1)
            .duration(60)
            .icon(atlas.findRegion("resistor")).build("resistor");
        TRANSISTOR = new Recipe.Builder()
            .input(Items.SILICON, 1)
            .input(Items.COPPER_WIRE, 2)
            .input(Items.IRON_PLATE, 1)
            .output(Items.TRANSISTOR, 1)
            .duration(120)
            .icon(atlas.findRegion("transistor")).build("transistor");
        ELECTRONIC_CIRCUIT = new Recipe.Builder()
            .input(Items.SILICON, 2)
            .input(Items.COPPER_WIRE, 4)
            .input(Items.INDUCTOR, 3)
            .input(Items.CAPACITOR, 3)
            .input(Items.RESISTOR, 3)
            .input(Items.TRANSISTOR, 3)
            .output(Items.ELECTRONIC_CIRCUIT, 1)
            .duration(240)
            .icon(atlas.findRegion("electronic_circuit")).build("electronic_circuit");

        for (Content content : ContentManager.getContentOfType(ContentType.RECIPE)) {
            Recipe recipe = (Recipe) content;
            all.add(recipe);
        }
    }
}
