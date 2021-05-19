package es.luepg.ecs.world.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateViewPositionPacket;
import es.luepg.ecs.event.entity.PlayerMoveEvent;
import es.luepg.ecs.event.entity.PlayerTrackChunkEvent;
import es.luepg.ecs.event.entity.PlayerUnTrackChunkEvent;
import es.luepg.ecs.world.entity.PlayerComponent;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;

import java.util.function.IntConsumer;

/**
 * Send the world (only blocks) to players
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Listener
public class PlayerSendWorldSystem extends BaseSystem {

    private ComponentMapper<PlayerComponent> playerComponentComponentMapper;


    @Handler
    public void onMove(PlayerMoveEvent event) {
        int vd = event.getWorld().getViewDistance() + 1;

        int tx = toChunk(event.getTo().getX()),
                tz = toChunk(event.getTo().getZ());
        int fx = toChunk(event.getFrom().getX()),
                fz = toChunk(event.getFrom().getZ());

        if (tx == fx && fz == tz)
            return; // Skip here


        PlayerComponent playerComponent = playerComponentComponentMapper.get(event.getPlayer());

        // make sure to update the view position
        playerComponent.getSession().send(new ServerUpdateViewPositionPacket(tx, tz));

        MBassador<Object> eventBus = event.getWorld().getServer().getEventBus();

        this.handle(fx, tx, vd, loadChunkX -> {
                    for (int zo = -vd; zo <= vd; zo++) {
                        Column c = event.getWorld().getChunkProvider()
                                .provideColumnAtChunkCoords(loadChunkX, zo + tz);
                        playerComponent.getSession().send(new ServerChunkDataPacket(c));
                        eventBus.publish(new PlayerTrackChunkEvent(event.getPlayer(), event.getWorld(), c));
                    }
                },
                unloadChunkX -> {
                    for (int zo = -vd; zo <= vd; zo++) {
                        playerComponent.getSession().send(new ServerUnloadChunkPacket(unloadChunkX, zo + tz));
                        eventBus.publish(new PlayerUnTrackChunkEvent(event.getPlayer(), event.getWorld(), unloadChunkX, zo + tz));

                    }
                }
        );

        // Z direction too
        this.handle(fz, tz, vd, loadChunkZ -> {
                    for (int o = -vd; o <= vd; o++) {
                        Column c = event.getWorld().getChunkProvider()
                                .provideColumnAtChunkCoords(o + tx, loadChunkZ);
                        playerComponent.getSession().send(new ServerChunkDataPacket(c));
                        eventBus.publish(new PlayerTrackChunkEvent(event.getPlayer(), event.getWorld(), c));

                    }
                },
                unloadChunkZ -> {
                    for (int o = -vd; o <= vd; o++) {
                        playerComponent.getSession().send(new ServerUnloadChunkPacket(o + tx, unloadChunkZ));
                        eventBus.publish(new PlayerUnTrackChunkEvent(event.getPlayer(), event.getWorld(), o + tx, unloadChunkZ));
                    }
                }
        );


    }


    private int toChunk(double blockPos) {
        return ((int) blockPos) >> 4;
    }

    private void handle(int from, int to, int vd, IntConsumer loader, IntConsumer unloader) {
        int dir = to > from ? 1 : -1;

        dir *= vd;

        int newMax = to + dir;
        int newMin = minMax(dir, to - dir, from + dir);

        // New chunks
        for (int x = Math.min(newMin, newMax) + 1, xborder = Math.max(newMin, newMax); x <= xborder; x++) {
            loader.accept(x);
        }

        int xOldMax = minMax(-dir, from + dir, to - dir);
        int xOldMin = from - dir;


        // Unload chunks
        for (int x = Math.min(xOldMin, xOldMax) + 1, xborder = Math.max(xOldMin, xOldMax); x <= xborder; x++) {
            unloader.accept(x);
        }

    }

    private int minMax(int dir, int a, int b) {
        if (dir > 0)
            return Math.max(a, b);
        return Math.min(a, b);
    }

    @Override
    protected void processSystem() {

    }

    @Override
    protected boolean checkProcessing() {
        return false;
    }
}
