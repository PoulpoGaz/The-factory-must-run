package fr.poulpogaz.run.factory.item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import fr.poulpogaz.run.Content;
import fr.poulpogaz.run.ContentManager;
import fr.poulpogaz.run.ContentType;

public class Items {

    public static Item IRON_PLATE;
    public static Item GEAR;
    public static Item PIPE;

    public static Item CAPACITOR;
    public static Item COPPER_PLATE;
    public static Item COPPER_WIRE;
    public static Item ELECTRONIC_CIRCUIT;
    public static Item INDUCTOR;
    public static Item RESISTOR;
    public static Item SILICON;
    public static Item TRANSISTOR;
    public static Item WOOD;
    public static Item WOOD_PLANK;
    public static Item CARBON;

    public static Array<Item> generable = new Array<>();
    public static Array<Item> all = new Array<>();

    public static void load() {
        WOOD = new Item("wood");
        WOOD.canBeGenerated = true;
        WOOD.value = 1;
        WOOD_PLANK = new Item("wood_plank");
        WOOD_PLANK.value = 25;

        IRON_PLATE = new Item("iron_plate");
        IRON_PLATE.canBeGenerated = true;
        IRON_PLATE.value = 1000;
        GEAR = new Item("gear");
        GEAR.value = 1200;
        PIPE = new Item("pipe");
        PIPE.value = 1200;

        COPPER_PLATE = new Item("copper_plate");
        COPPER_PLATE.canBeGenerated = true;
        COPPER_PLATE.value = 1000;
        COPPER_WIRE = new Item("copper_wire");
        COPPER_WIRE.value = 1200;

        CAPACITOR = new Item("capacitor");
        CAPACITOR.value = 10000;



        ELECTRONIC_CIRCUIT = new Item("electronic_circuit");
        ELECTRONIC_CIRCUIT.value = 100000;

        INDUCTOR = new Item("inductor");
        INDUCTOR.value = 10000;

        RESISTOR = new Item("resistor");
        RESISTOR.value = 10000;

        SILICON = new Item("silicon");
        SILICON.canBeGenerated = true;
        SILICON.value = 40000;
        TRANSISTOR = new Item("transistor");
        TRANSISTOR.value = 50000;

        CARBON = new Item("carbon");
        CARBON.canBeGenerated = true;
        CARBON.value = 2000;

        for (Content content : ContentManager.getContentOfType(ContentType.ITEM)) {
            Item item = (Item) content;

            if (item.canBeGenerated()) {
                generable.add(item);
            }
        }
    }
}
