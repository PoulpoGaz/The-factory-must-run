package fr.poulpogaz.run.factory.item;

public class Items {

    public static Item IRON_PLATE;
    public static Item GEAR;
    public static Item PIPE;

    public static void load() {
        IRON_PLATE = new Item("iron_plate");
        GEAR = new Item("gear");
        PIPE = new Item("pipe");
    }
}
