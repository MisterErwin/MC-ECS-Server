package es.luepg.ecs.data.game.world.block.state;

import java.util.Collection;
import java.util.Optional;

public interface IBlockProperty<T extends Comparable<T>> {
    String getName();

    Collection<T> getAllowedValues();

    Optional<T> parseValue(String value);
}
