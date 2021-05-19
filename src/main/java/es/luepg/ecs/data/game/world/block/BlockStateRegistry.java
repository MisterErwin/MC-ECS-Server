package es.luepg.ecs.data.game.world.block;

import es.luepg.ecs.data.game.world.block.registry.RegistryArrayImpl;
import es.luepg.ecs.data.game.world.block.state.IBlockState;
import es.luepg.mcdata.data.game.world.block.Blocks;

import javax.annotation.Nonnull;

public class BlockStateRegistry extends RegistryArrayImpl<IBlockState> {

//    private final static int MAX_BLOCK_DATA = 11270 + 1;


  public final static BlockStateRegistry INSTANCE = new BlockStateRegistry();

  private IBlockState AIR;


  private BlockStateRegistry() {
    super(Blocks.MAX_BLOCK_STATE_ID + 1);
  }

  @Override
  @Nonnull
  public IBlockState getRegistryEntryByIndex(int index) {
    IBlockState ret = super.getRegistryEntryByIndex(index);
    if (ret == null) {
      return AIR;
    }
    return ret;
  }

  @Override
  public void register(int index, IBlockState entry) {
    super.register(index, entry);
    if (index == 0) {
      this.AIR = entry;
    }
  }
}
