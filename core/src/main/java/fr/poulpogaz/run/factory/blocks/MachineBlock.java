package fr.poulpogaz.run.factory.blocks;

public class MachineBlock extends Block {

    public MachineBlock(String name) {
        super(name);
    }

    @Override
    public void load() {

    }

    @Override
    public int width() {
        return 2;
    }

    @Override
    public int height() {
        return 2;
    }
}
