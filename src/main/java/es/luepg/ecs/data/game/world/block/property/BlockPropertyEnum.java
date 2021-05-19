package es.luepg.ecs.data.game.world.block.property;

import com.google.common.collect.ImmutableSet;

import java.util.*;

public class BlockPropertyEnum<T extends Enum<T> & IBlockPropertyEnum> extends BlockPropertyHelper<T> {
  private final ImmutableSet<T> allowedValues;
  private final Map<String, T> nameMapping = new HashMap<>();

  protected BlockPropertyEnum(String name, Class<T> enuMClass, Collection<T> allowedValues) {
    super(name, enuMClass);

    this.allowedValues = ImmutableSet.copyOf(allowedValues);

    for (T allowedValue : allowedValues) {
      this.nameMapping.put(allowedValue.getName(), allowedValue);
    }
  }

  @Override
  public Collection<T> getAllowedValues() {
    return this.allowedValues;
  }

  @Override
  public Optional<T> parseValue(String value) {
    return Optional.ofNullable(this.nameMapping.get(value));
  }


  public static <T extends Enum<T> & IBlockPropertyEnum> BlockPropertyEnum<T> create(String name, Class<T> tEnum, Collection<T> values) {
    return new BlockPropertyEnum<T>(name, tEnum, values);
  }

  public static <T extends Enum<T> & IBlockPropertyEnum> BlockPropertyEnum<T> create(String name, Class<T> tEnum, T... values) {
    return create(name, tEnum, Arrays.asList(values));
  }

  public static <T extends Enum<T> & IBlockPropertyEnum> BlockPropertyEnum<T> create(String name, Class<T> tEnum) {
    return create(name, tEnum, tEnum.getEnumConstants());
  }
}
