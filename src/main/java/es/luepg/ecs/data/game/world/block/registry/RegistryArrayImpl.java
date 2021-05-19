package es.luepg.ecs.data.game.world.block.registry;

import java.util.Arrays;
import java.util.Iterator;

@SuppressWarnings("unchecked")
public abstract class RegistryArrayImpl<T> implements IRegistry<T> {
    private final Object[] data;
    private int head;

    public RegistryArrayImpl(int maxData) {
        this.data = new Object[maxData];
    }

    @Override
    public int getRegistryIndex(T t) {
        for (int i = 0; i < data.length; i++) {
            if (t.equals(data[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public T getRegistryEntryByIndex(int index) {
        if (index < 0 || index >= this.data.length) {
            return null;
        }
        return (T) this.data[index];
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.stream(data).map(x -> (T) x).iterator();
    }

    @Override
    public void register(T entry) {
        while (this.head < this.data.length && this.data[this.head] != null) {
            this.head++;
        }
        if (this.head >= this.data.length || this.data[this.head] != null) {
            throw new IllegalArgumentException("The registry is full!");
        }

        this.data[this.head++] = entry;
    }

    @Override
    public void register(int index, T entry) {
        if (index >= this.data.length) {
            throw new IllegalArgumentException("Index " + index + " is out of bounds (" + this.data.length + ")");
        }
        if (this.data[index] != null) {
            throw new IllegalArgumentException("The index is already taken");
        }

        this.data[index] = entry;
        if (index == this.head) {
            this.head++;
        }
    }
}
