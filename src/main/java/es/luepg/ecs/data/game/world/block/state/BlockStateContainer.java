package es.luepg.ecs.data.game.world.block.state;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import es.luepg.ecs.data.game.world.block.Block;

import java.util.Collection;

public class BlockStateContainer {
  private final Block block;
  private final ImmutableMap<String, IBlockProperty<?>> properties;
  private final ImmutableList<IBlockState> validStates;

  public BlockStateContainer(Block block, ImmutableMap<String, IBlockProperty<?>> properties, ImmutableList<IBlockState> validStates) {
    this.block = block;
    this.properties = properties;
    this.validStates = validStates;
  }

  public Collection<IBlockState> getValidStates() {
    return this.validStates;
  }


}
