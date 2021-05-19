package es.luepg.ecs.world.systems.blocks;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import es.luepg.ecs.data.game.world.block.state.IBlockState;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerActionPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerPlaceBlockPacketReceivedEvent;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.entity.PlayerInventoryComponent;
import es.luepg.ecs.world.systems.ChunkTrackerSystem;
import es.luepg.ecs.world.systems.PlayerEntityMapper;
import es.luepg.mcdata.data.game.world.Material;
import es.luepg.mcdata.data.game.world.block.Blocks;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;

/**
 * A simple block placing & destroying system for creative mode players
 * <p>
 * Syncs the block change to all players
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Listener
public class SimpleBlockSystem extends BaseSystem {

  private PlayerEntityMapper playerEntityMapper;
  private ChunkTrackerSystem chunkTrackerSystem;
  private ComponentMapper<PlayerInventoryComponent> playerInventoryComponentMapper;

  @Override
  protected boolean checkProcessing() {
    return false;
  }

  @Override
  protected void processSystem() {
  }

  @Handler
  public void onPlaceBlock(ClientPlayerPlaceBlockPacketReceivedEvent event) {
    if (event.getWorld() == null || event.getWorld().getArtemisWorld() != this.getWorld()) return;

    Entity player = event.getPlayerEntity();
    if (player == null) {
      return; // Other world?
    }
    World rw = event.getWorld();

    Position pos = event.getPacket().getPosition();

    PlayerInventoryComponent inventoryComponent = playerInventoryComponentMapper.get(player);

    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();

    switch (event.getPacket().getFace()) {
      case EAST:
        x++;
        break;
      case WEST:
        x--;
        break;
      case SOUTH:
        z++;
        break;
      case NORTH:
        z--;
        break;
      case UP:
        y++;
        break;
      case DOWN:
        y--;
        break;
    }

    Material material;

    if (inventoryComponent != null) {
      material = inventoryComponent.getSelectedItem();

      if (material.getBlock() == null) {
        material = Material.AIR;
      }
    } else {
      material = Material.RED_WOOL;
    }


    IBlockState blockState = material.getBlock().getDefaultState();

    // TODO: An event here?

    rw.getChunkProvider().setBlockState(
            x, y, z,
            blockState
    );

    chunkTrackerSystem.broadcastTo(x >> 4, z >> 4,
            new ServerBlockChangePacket(
                    new BlockChangeRecord(
                            new Position(x, y, z),
                            blockState.getGlobalPaletteIndex()
                    )
            )
    );

  }

  @Handler
  public void onBreakBlock(ClientPlayerActionPacketReceivedEvent event) {
    if (event.getWorld() == null || event.getWorld().getArtemisWorld() != this.getWorld()) return;
    if (event.getPacket().getAction() == PlayerAction.START_DIGGING) {
      Entity player = event.getPlayerEntity();
      if (player == null) {
        return; // Other world?
      }
      World rw = event.getWorld();
      Position pos = event.getPacket().getPosition();

      rw.getChunkProvider().setBlockState(
              pos.getX(),
              pos.getY(),
              pos.getZ(),
              Blocks.AIR.getDefaultState()
      );

      chunkTrackerSystem.broadcastTo(
              pos.getX() >> 4, pos.getZ() >> 4,
              new ServerBlockChangePacket(
                      new BlockChangeRecord(
                              pos,
                              Blocks.AIR.getDefaultState().getGlobalPaletteIndex()
                      )
              )
      );

    }
  }

}
