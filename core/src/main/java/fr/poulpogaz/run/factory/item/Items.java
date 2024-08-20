package fr.poulpogaz.run.factory.item;

public class Items {

    public static Item ironPlate;
    public static Item gear;
    public static Item pipe;

    public static void load() {
        ironPlate = new Item("iron_plate");
        gear = new Item("gear");
        pipe = new Item("pip");
    }
}
