package fr.poulpogaz.run.factory.blocks;

public class Blocks {

    public static Block AIR;

    public static Block CONVEYOR;
    public static Block UNDERGROUND_CONVEYOR;
    public static Block ROUTER;
    public static Block MACHINE;
    public static Block WALL;
    public static Block GENERATOR;
    public static Block CONSUMER;

    public static void load() {
        AIR = new AirBlock();

        CONVEYOR = new ConveyorBlock("conveyor");
        UNDERGROUND_CONVEYOR = new UndergroundConveyorBlock("underground_conveyor");
        ROUTER = new RouterBlock("router");

        MACHINE = new MachineBlock();

        WALL = new WallBlock();
        GENERATOR = new GeneratorBlock("generator");
        CONSUMER = new ConsumerBlock();
    }
}
