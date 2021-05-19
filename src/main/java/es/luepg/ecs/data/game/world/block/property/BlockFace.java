package es.luepg.ecs.data.game.world.block.property;

public enum BlockFace implements IBlockPropertyEnum {
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST,
    SPECIAL;

    @Override
    public String getName() {
        return this.name().toLowerCase();
    }
}
