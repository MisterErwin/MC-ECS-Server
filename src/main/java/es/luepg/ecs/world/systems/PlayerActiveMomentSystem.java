package es.luepg.ecs.world.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerMovementPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import es.luepg.ecs.event.entity.EntityChangeChunkEvent;
import es.luepg.ecs.event.entity.PlayerMoveEvent;
import es.luepg.ecs.packetwrapper.PlayerPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerPositionPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerPositionRotationPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerRotationPacketReceivedEvent;
import es.luepg.ecs.world.entity.HeadingComponent;
import es.luepg.ecs.world.entity.LocatedComponent;
import es.luepg.ecs.world.util.Location;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;


/**
 * Handle movement of players done by the player
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Listener
public class PlayerActiveMomentSystem extends BaseSystem {

    private ComponentMapper<LocatedComponent> locatedComponentComponentMapper;
    private ComponentMapper<HeadingComponent> mHeading;

    private PlayerEntityMapper playerEntityMapper;


    @Handler
    public void onMove(ClientPlayerPositionPacketReceivedEvent event) {
        if (event.getWorld() == null || event.getWorld().getArtemisWorld() != this.getWorld()) return;

        this.onMove_(event);
    }

    @Handler
    public void onMoveRotate(ClientPlayerPositionRotationPacketReceivedEvent event) {
        if (event.getWorld() == null || event.getWorld().getArtemisWorld() != this.getWorld()) return;

        this.onMove_(event);
        this.onRotate_(event);
    }

    @Handler
    public void onRotate(ClientPlayerRotationPacketReceivedEvent event) {
        if (event.getWorld() == null || event.getWorld().getArtemisWorld() != this.getWorld()) return;

        this.onRotate_(event);
    }

    private void onMove_(PlayerPacketReceivedEvent<? extends ClientPlayerMovementPacket> event) {
        LocatedComponent locatedComponent = locatedComponentComponentMapper.get(event.getPlayerEntity());

        Location from = locatedComponent.getLocation();

        Location to = new Location(event.getPacket().getX(),
                event.getPacket().getY(),
                event.getPacket().getZ());

        if (from.distanceSquared(to) > 5 * 5) {
            System.out.println("Fancy movement to " + to + " from " + from);
            event.getSession().send(
                    new ServerPlayerPositionRotationPacket(
                            from.getX(), from.getY(), from.getZ(),
                            0, 0,
                            12
                    )
            );
            return;
        }

        PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(event.getPlayerEntity(), event.getWorld(),
                locatedComponent.getLocation(), to);


        playerMoveEvent.getWorld().getServer().getEventBus().publish(playerMoveEvent);

        locatedComponent.setLocation(to);

        if ((int) from.getX() >> 4 != (int) to.getX() >> 4 || (int) from.getZ() >> 4 != (int) to.getZ() >> 4) {
            // Chunk change event
            playerMoveEvent.getWorld().getServer().getEventBus().publish(new EntityChangeChunkEvent(event.getPlayerEntity(),
                    event.getWorld(),
                    (int) from.getX() >> 4, (int) from.getZ() >> 4,
                    (int) to.getX() >> 4, (int) to.getZ() >> 4));


        }
    }

    private void onRotate_(PlayerPacketReceivedEvent<? extends ClientPlayerMovementPacket> event) {
        HeadingComponent headingComponent = mHeading.get(event.getPlayerEntity());

        if (headingComponent == null) return;

        headingComponent.setYaw((float) event.getPacket().getYaw());
        headingComponent.setPitch((float) event.getPacket().getPitch());
    }

    @Override
    protected void processSystem() {

    }

    @Override
    protected boolean checkProcessing() {
        return false;
    }
}
