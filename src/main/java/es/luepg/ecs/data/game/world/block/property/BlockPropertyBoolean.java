package es.luepg.ecs.data.game.world.block.property;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Optional;

public class BlockPropertyBoolean extends BlockPropertyHelper<Boolean> {

    private final ImmutableSet<Boolean> allowedValues = ImmutableSet.of(true, false);

    private BlockPropertyBoolean(String name) {
        super(name, Boolean.class);
    }

    @Override
    public Collection<Boolean> getAllowedValues() {
        return this.allowedValues;
    }

    @Override
    public Optional<Boolean> parseValue(String value) {
        return "true".equals(value) ? Optional.of(true) : ("false".equals(value) ? Optional.of(false) : Optional.empty());
    }

    public static BlockPropertyBoolean create(String name) {
        return new BlockPropertyBoolean(name);
    }
}
