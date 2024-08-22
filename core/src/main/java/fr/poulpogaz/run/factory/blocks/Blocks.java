package fr.poulpogaz.run.factory.blocks;

public class Blocks {

    public static Block AIR;
    public static Block CONVEYOR;
    public static Block UNLOADING_CONVEYOR;
    public static Block ROUTER;
    public static Block WALL;
    public static Block GENERATOR;

    public static void load() {
        AIR = new AirBlock();
        CONVEYOR = new ConveyorBlock("conveyor");
        UNLOADING_CONVEYOR = new ConveyorBlock("unloading_conveyor");
        ROUTER = new RouterBlock("router");
        WALL = new WallBlock();
        GENERATOR = new GeneratorBlock("generator");
    }
}
