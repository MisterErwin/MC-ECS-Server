package es.luepg.ecs.data.game.world.block.registry;

public interface IRegistry<T> extends Iterable<T> {
    int getRegistryIndex(T t);

    T getRegistryEntryByIndex(int index);

    void register(T entry);

    void register(int index, T entry);
}
