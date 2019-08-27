package es.luepg.ecs.world.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.One;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.collect.ImmutableList;
import es.luepg.ecs.event.entity.EntityChangeChunkEvent;
import es.luepg.ecs.event.entity.PlayerTrackChunkEvent;
import es.luepg.ecs.event.entity.PlayerUnTrackChunkEvent;
import es.luepg.ecs.event.login.PlayerBuildEvent;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.entity.LocatedComponent;
import es.luepg.ecs.world.entity.PlayerComponent;
import es.luepg.ecs.world.entity.TrackingChunksComponent;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A system that keeps track of chunks a player is tracking and
 * entities inside a chunk
 *
 * @author elmexl
 * Created on 18.07.2019.
 */
@One(LocatedComponent.class)
@Listener
public class ChunkTrackerSystem extends BaseEntitySystem {

    private final LongObjectMap<ColumnReference> trackedChunkColumns = new LongObjectHashMap<>();

    private ComponentMapper<LocatedComponent> mLocated;
    private ComponentMapper<PlayerComponent> mPlayers;
    private ComponentMapper<TrackingChunksComponent> mTrackedChunks;

    @Override
    protected boolean checkProcessing() {
        return false;
    }

    @Override
    protected void processSystem() {
    }


    @Override
    public void inserted(int eid) {
        // Add entity to chunk
        if (mLocated.has(eid)) {
            LocatedComponent loc = mLocated.get(eid);
            ColumnReference chunk = getColumnReference(loc.getChunkX(), loc.getChunkZ());

            System.out.println("Adding entity to chunk: " + eid + ": " + loc.getChunkX() + "|" + loc.getChunkZ());
            chunk.entities.add(eid);
        }
    }

    @Override
    public void removed(int eid) {
        //  Trigger PlayerUnTrackChunkEvent when a player leaves
        if (mTrackedChunks.has(eid)) {
            System.out.println("Untracking chunks of removed player entitiy");
            TrackingChunksComponent trackingChunksComponent = mTrackedChunks.get(eid);


            World world = getWorld().getSystem(World.ReferenceSystem.class).getRealWorld();
            Entity entity = getWorld().getEntity(eid);

            // we make sure to copy the set first
            for (int[] chunkC : new HashSet<>(trackingChunksComponent.getTrackedChunks())) {
                world.getServer().getEventBus()
                        .publish(new PlayerUnTrackChunkEvent(entity, world, chunkC[0], chunkC[1]));
            }
        }

        // Remove entity from chunk
        if (mLocated.has(eid)) {
            LocatedComponent loc = mLocated.get(eid);
            ColumnReference chunk = getColumnReference(loc.getChunkX(), loc.getChunkZ());

            System.out.println("Removing eid from chunk " + eid + "// " + loc.getChunkX() + "|" + loc.getChunkZ());
            chunk.entities.remove(eid);
        }

    }

    @Handler
    public void onEntityChangeChunk(EntityChangeChunkEvent event) {
        // Add entity to new chunk
        ColumnReference chunk = getColumnReference(event.getToChunkX(), event.getToChunkZ());
        chunk.entities.add(event.getEntity().getId());

        chunk = getColumnReference(event.getFromChunkX(), event.getFromChunkZ());
        chunk.entities.remove(event.getEntity().getId());
    }

    @Handler
    public void onTrackChunk(PlayerTrackChunkEvent event) {
        // Mark that the player is being subscribed to a chunk
        ColumnReference chunk = getColumnReference(event.getChunk().getX(), event.getChunk().getZ());

        chunk.subscribingPlayers.add(event.getPlayer().getId());

        mTrackedChunks
                .get(event.getPlayer())
                .getTrackedChunks().add(new int[]{event.getChunk().getX(), event.getChunk().getZ()});
    }

    @Handler
    public void onUntrackChunk(PlayerUnTrackChunkEvent event) {
        // Unmark the subscription
        ColumnReference chunk = getColumnReference(event.getChunkX(), event.getChunkZ());

        chunk.subscribingPlayers.remove(event.getPlayer().getId());

        mTrackedChunks
                .get(event.getPlayer())
                .getTrackedChunks().remove(new int[]{event.getChunkX(), event.getChunkZ()});


        // prepare chunk unloading
        if (chunk.canBeUnloaded()) {
            // ToDo: Move the unloading elsewhere (an own system),
            // just pass the information that the chunk is no longer watched to it
            // ToDO: And make a ChunkUnloadEvent
            world.getSystem(World.ReferenceSystem.class).getRealWorld()
                    .getChunkProvider()
                    .doUnloadChunk(event.getChunkX(), event.getChunkZ());
            this.trackedChunkColumns.remove(posToLong(event.getChunkX(), event.getChunkZ()));
        }
    }

    /**
     * Broadcast a packet to every player subscribing to a chunk
     *
     * @param chunkX the x chunk coordinate (/16 yada yada)
     * @param chunkZ the z chunk coordinate (/16 yada yada)
     * @param packet the packet to send
     */
    public void broadcastTo(int chunkX, int chunkZ, Packet packet) {
        for (int eid : getColumnReference(chunkX, chunkZ).subscribingPlayers) {
            mPlayers.get(eid).getSession().send(packet);
        }
    }

    /**
     * Broadcast a packet to every player subscribing to a chunk except a players
     * <p>
     * Same as {@link #broadcastTo(int, int, Packet, int...)}, just without array creation
     *
     * @param chunkX       the x chunk coordinate (/16 yada yada)
     * @param chunkZ       the z chunk coordinate (/16 yada yada)
     * @param packet       the packet to send
     * @param exceptPlayer the player excluded
     */
    public void broadcastTo(int chunkX, int chunkZ, Packet packet, int exceptPlayer) {
        for (int eid : getColumnReference(chunkX, chunkZ).subscribingPlayers) {
            if (exceptPlayer != eid)
                mPlayers.get(eid).getSession().send(packet);
        }
    }

    /**
     * Broadcast a packet to every player subscribing to a chunk except a list of players
     *
     * @param chunkX        the x chunk coordinate (/16 yada yada)
     * @param chunkZ        the z chunk coordinate (/16 yada yada)
     * @param packet        the packet to send
     * @param exceptPlayers the players excluded
     */
    public void broadcastTo(int chunkX, int chunkZ, Packet packet, int... exceptPlayers) {
        for (int eid : getColumnReference(chunkX, chunkZ).subscribingPlayers) {
            if (!ArrayUtils.contains(exceptPlayers, eid))
                mPlayers.get(eid).getSession().send(packet);
        }
    }

    //ToDo: Is ImmutableList really needed?

    /**
     * Get the entities in that chunk.
     * Do NOT modify the data
     *
     * @param chunkX the x chunk coordinate (/16 yada yada)
     * @param chunkZ the z chunk coordinate (/16 yada yada)
     * @return a collection of all entities of this chunk
     */
    public List<Integer> getEntities(int chunkX, int chunkZ) {
        return ImmutableList.copyOf(getColumnReference(chunkX, chunkZ).entities);
    }

    /**
     * Get the difference of players tracking two chunks
     *
     * @param chunkAX chunk A x coordinate
     * @param chunkAZ chunk A z coordinate
     * @param chunkBX chunk B x coordinate
     * @param chunkBZ chunk B z coordinate
     * @param onlyInA a consumer for all player entity ids that only track A and not B
     * @param onlyInB a consumer for all player entity ids that only track B and not A
     */
    public void getDifferenceInTrackingPlayers(int chunkAX, int chunkAZ, int chunkBX, int chunkBZ, Consumer<Integer> onlyInA, Consumer<Integer> onlyInB) {
        ColumnReference a = getColumnReference(chunkAX, chunkAZ);
        ColumnReference b = getColumnReference(chunkBX, chunkBZ);

        Set<Integer> bList = new HashSet<>(b.subscribingPlayers);
        for (int i : a.subscribingPlayers) {
            if (!bList.remove(i)) {
                onlyInA.accept(i);
            }
        }
        bList.forEach(onlyInB);
    }

    @Handler
    public void onPlayerCreate(PlayerBuildEvent event) {
        // If this system is in use, we add a TrackingChunksComponent to every new player
        event.with(new TrackingChunksComponent());
    }


    private ColumnReference getColumnReference(int chunkX, int chunkZ) {
        long l = posToLong(chunkX, chunkZ);
        ColumnReference ref = this.trackedChunkColumns.get(l);

        if (ref == null) {
            // Start tracking that chunk
            ref = new ColumnReference();
            this.trackedChunkColumns.put(l, ref);
        }
        return ref;
    }

    private static long posToLong(int chunkX, int chunkZ) {
        return (long) chunkX & 4294967295L | ((long) chunkZ & 4294967295L) << 32;
    }

    /**
     * Data class
     */
    public class ColumnReference {
        /**
         * The collection of players that are tracking this chunk
         */
        private Set<Integer> subscribingPlayers = new HashSet<>();

        /**
         * The entities currently in this chunk
         */
        private Set<Integer> entities = new HashSet<>();

        public boolean canBeUnloaded() {
            // ToDo: Remove the entities check! (once chunk saving is done)
            return subscribingPlayers.isEmpty() && entities.isEmpty();
        }
    }
}
