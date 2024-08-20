package fr.poulpogaz.run.factory.blocks;

public class Blocks {

    public static Block CONVEYOR;
    public static Block ROUTER;
    public static Block WALL;

    public static void load() {
        CONVEYOR = new ConveyorBlock("conveyor");
        ROUTER = new ConveyorBlock("router");
        WALL = new WallBlock();
    }
}
