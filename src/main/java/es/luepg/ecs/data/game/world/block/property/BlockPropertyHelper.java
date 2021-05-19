package es.luepg.ecs.data.game.world.block.property;

import es.luepg.ecs.data.game.world.block.state.IBlockProperty;

import java.util.Objects;

public abstract class BlockPropertyHelper<T extends Comparable<T>> implements IBlockProperty<T> {
    private final String name;
    private final Class<T> clazz;

    protected BlockPropertyHelper(String name, Class<T> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    @Override
    public String getName() {
        return name;
    }

    protected Class<T> getClazz() {
        return clazz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPropertyHelper<?> that = (BlockPropertyHelper<?>) o;
        return getName().equals(that.getName()) &&
                getClazz().equals(that.getClazz());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getClazz());
    }
}
