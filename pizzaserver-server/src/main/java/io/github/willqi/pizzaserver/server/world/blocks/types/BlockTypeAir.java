package io.github.willqi.pizzaserver.server.world.blocks.types;

public class BlockTypeAir extends BlockType {

    @Override
    public String getBlockId() {
        return BlockTypeID.AIR;
    }

    @Override
    public String getName() {
        return "Air";
    }

}
