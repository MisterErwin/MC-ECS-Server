package es.luepg.ecs.data.game.world.block.property;

import com.google.common.collect.ImmutableSet;

import java.util.*;

public class BlockPropertyInteger extends BlockPropertyHelper<Integer> {
    private final ImmutableSet<Integer> allowedValues;

    private BlockPropertyInteger(String name, Collection<Integer> allowedValues) {
        super(name, Integer.class);

        this.allowedValues = ImmutableSet.copyOf(allowedValues);
    }

    @Override
    public Collection<Integer> getAllowedValues() {
        return this.allowedValues;
    }

    @Override
    public Optional<Integer> parseValue(String value) {
        try {
            Integer integer = Integer.parseInt(value);
            return this.allowedValues.contains(integer) ? Optional.of(integer) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }


    public static BlockPropertyInteger create(String name, Collection<Integer> allowedValues) {
        return new BlockPropertyInteger(name, allowedValues);
    }

    public static BlockPropertyInteger create(String name, Integer... allowedValues) {
        return create(name, Arrays.asList(allowedValues));
    }
}
