package es.luepg.ecs.world.systems.inventory;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.World;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerChangeHeldItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCreativeInventoryActionPacket;
import es.luepg.ecs.event.login.PlayerBuildEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerChangeHeldItemPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.window.ClientCreativeInventoryActionPacketReceivedEvent;
import es.luepg.ecs.world.entity.PlayerInventoryComponent;
import es.luepg.ecs.world.systems.PlayerEntityMapper;
import es.luepg.mcdata.data.game.world.Material;
import net.engio.mbassy.listener.Handler;

/**
 * A system to track creative inventories (at least somewhat)
 *
 * @author elmexl
 * Created on 20.07.2019.
 */
public class PlayerInventorySystem extends BaseSystem {
    private ComponentMapper<PlayerInventoryComponent> playerInventoryComponentMapper;
    private PlayerEntityMapper playerEntityMapper;

    @Override
    protected void setWorld(World world) {
        super.setWorld(world);
        // Attempts to get a small performance gain
        world.getInvocationStrategy().setEnabled(this, false);
    }

    @Handler
    public void onItemChangeCreative(ClientCreativeInventoryActionPacketReceivedEvent event) {
        if (event.getWorld() == null || event.getWorld().getArtemisWorld() != this.getWorld()) return;

        Entity player = playerEntityMapper.getEntityForPlayer(event.getSession());
        if (player == null) return;
        ClientCreativeInventoryActionPacket actionPacket = event.getPacket();
        PlayerInventoryComponent inventory = playerInventoryComponentMapper.get(player);

        if (actionPacket.getClickedItem() == null) {
            inventory.getInventory()[actionPacket.getSlot()] = Material.AIR;
        } else {
            Material m = Material.byItemProtocolID(actionPacket.getClickedItem().getId());
            System.out.println(actionPacket.getClickedItem().getId());
            System.out.println("Setting slot to " + m.getName());
            inventory.getInventory()[actionPacket.getSlot()] = m;
        }
        //ToDo: Inventory Events
    }

    @Handler
    public void onHeldItemChange(ClientPlayerChangeHeldItemPacketReceivedEvent event) {
        if (event.getWorld() == null || event.getWorld().getArtemisWorld() != this.getWorld()) return;

        if (event.getPlayerEntity() == null) return;
        PlayerInventoryComponent inventory = playerInventoryComponentMapper.get(event.getPlayerEntity());

        inventory.setSelectedHotBarIndex(event.getPacket().getSlot());
        //ToDo: Event
    }

    @Handler
    public void onCreatePlayer(PlayerBuildEvent event) {
        // Add our inventory component
        event.with(new PlayerInventoryComponent());
    }


    @Override
    protected void processSystem() {
    }

    @Override
    protected boolean checkProcessing() {
        return false;
    }
}
