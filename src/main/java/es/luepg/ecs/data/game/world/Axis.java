package es.luepg.ecs.data.game.world;

import es.luepg.ecs.data.game.world.block.property.IBlockPropertyEnum;

public enum Axis implements IBlockPropertyEnum {
    X("x"),
    Y("y"),
    Z("z"),
    NONE("none");

    private final String name;

    Axis(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
