package es.luepg.ecs.data.game.world.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import es.luepg.ecs.data.game.world.block.state.BlockStateContainer;
import es.luepg.ecs.data.game.world.block.state.IBlockProperty;
import es.luepg.ecs.data.game.world.block.state.IBlockState;

import java.util.*;
import java.util.function.Supplier;

public class Block {
    private final String name;
    private IBlockState defaultState;
    private BlockStateContainer blockStateContainer;

    protected Block(String name) {
        this.name = name;
    }

    public IBlockState getDefaultState() {
        return this.defaultState;
    }

    public String getName() {
        return name;
    }

    public BlockStateContainer getBlockStateContainer() {
        return blockStateContainer;
    }


    // Builder stuff

    public static Builder of(String name) {
        return new Builder(() -> new Block(name));
    }

    public static Builder of(java.util.function.Supplier<? extends Block> constructor) {
        return new Builder(constructor);
    }

    public static class Builder {
        private final java.util.function.Supplier<? extends Block> construct;
        private final List<BlockStateBuilder> states = new ArrayList<>();
        private final Map<String, IBlockProperty<?>> properties = new HashMap<>();

        Builder(Supplier<? extends Block> construct) {
            this.construct = construct;
        }

        public BlockStateBuilder addState(int index) {
            return new BlockStateBuilder(index, false);
        }

        public BlockStateBuilder
        setDefaultState(int index) {
            return new BlockStateBuilder(index, true);
        }

        //        public Builder

        public Block build() {
            Block b = construct.get();


            List<IBlockState> blockStates = new ArrayList<>();

            for (BlockStateBuilder blockState : this.states) {
                IBlockState state = blockState.__build(b);
                if (blockState.defProp) {
                    b.defaultState = state;
                }
                blockStates.add(state);
                BlockStateRegistry.INSTANCE.register(state.getGlobalPaletteIndex(), state);
            }

            if (b.defaultState == null)
                b.defaultState = blockStates.iterator().next();

            b.blockStateContainer = new BlockStateContainer(b,
                    ImmutableMap.copyOf(this.properties),
                    ImmutableList.copyOf(blockStates));
            return b;
        }

        public class BlockStateBuilder {
            private final int index;
            private final Map<IBlockProperty<?>, Comparable<?>> blockPropertyMap = new HashMap<>();
            private final boolean defProp;

            private BlockStateBuilder(int index, boolean defProp) {
                this.index = index;
                this.defProp = defProp;
            }

            public BlockStateBuilder with(IBlockProperty<?> property, String val) {
                Optional<? extends Comparable<?>> value = property.parseValue(val);
                this.blockPropertyMap.put(property,
                        value.orElseThrow(() -> new IllegalArgumentException("Unknown value of BlockProperty " + val)));
                return this;
            }

            public <T extends Comparable<T>> BlockStateBuilder with(IBlockProperty<T> property, T val) {
                this.blockPropertyMap.put(property, val);
                return this;
            }

            public Builder build() {
                Builder.this.states.add(this);
                for (IBlockProperty property : this.blockPropertyMap.keySet()) {
                    Builder.this.properties.putIfAbsent(property.getName(), property);
                }
                return Builder.this;
            }

            private BlockStateImpl __build(Block b) {
                return new BlockStateImpl(b, ImmutableMap.copyOf(this.blockPropertyMap), index);
            }
        }
    }


    private final static class BlockStateImpl implements IBlockState {
        private final Block block;
        private final ImmutableMap<IBlockProperty<?>, Comparable<?>> properties;
        private final int globalPaletteIndex;

        BlockStateImpl(Block block, ImmutableMap<IBlockProperty<?>, Comparable<?>> properties, int globalPaletteIndex) {
            this.block = block;
            this.properties = properties;
            this.globalPaletteIndex = globalPaletteIndex;
        }

        @Override
        public Collection<IBlockProperty<?>> getPropertyKeys() {
            return this.properties.keySet();
        }

        @Override
        public <T extends Comparable<T>> T getProperty(IBlockProperty<T> property) {
            return (T) this.properties.get(property);
        }

        @Override
        public ImmutableMap<IBlockProperty<?>, Comparable<?>> getProperties() {
            return this.properties;
        }

        @Override
        public Block getBlock() {
            return block;
        }

        @Override
        public int getGlobalPaletteIndex() {
            return globalPaletteIndex;
        }

        @Override
        public <T extends Comparable<T>> IBlockState getWithProperty(IBlockProperty<T> property, T val) {
            for (IBlockState ibs : this.getBlock().getBlockStateContainer().getValidStates()) {
                if (!ibs.getProperty(property).equals(val))
                    continue;
                for (Map.Entry<IBlockProperty<?>, Comparable<?>> e : this.properties.entrySet()) {
                    if (e.getKey().equals(property)) continue; // skip the one different property
                    // And check all other props
                    if (!e.getValue().equals(ibs.getProperty(e.getKey())))
                        continue;
                    return ibs;
                }
            }

            return this.getBlock().getDefaultState();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockStateImpl that = (BlockStateImpl) o;
            return getGlobalPaletteIndex() == that.getGlobalPaletteIndex() &&
                    getBlock().equals(that.getBlock()) &&
                    getProperties().equals(that.getProperties());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getBlock(), getProperties(), getGlobalPaletteIndex());
        }
    }


}
