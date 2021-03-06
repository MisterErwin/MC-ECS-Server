package es.luepg.ecs.world.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.annotations.One;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerRemoveEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnLivingEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import es.luepg.ecs.event.entity.EntityChangeChunkEvent;
import es.luepg.ecs.event.entity.PlayerTrackChunkEvent;
import es.luepg.ecs.event.entity.PlayerUnTrackChunkEvent;
import es.luepg.ecs.world.entity.*;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.kyori.adventure.text.Component;


/**
 * This system handles spawn & despawn packets of entities
 *
 * @author elmexl
 * Created on 25.08.2019.
 */
@All({LocatedComponent.class, UUIDComponent.class}) // We require the entity to have a location and an uuid
@One({EntityTypeMobComponent.class, PlayerComponent.class}) // And it has to be either a mob or player
@Listener
public class EntityShowMobSystem extends BaseEntitySystem {

    private ComponentMapper<LocatedComponent> mLocated;
    private ComponentMapper<GravityAffectedComponent> mGravity;
    private ComponentMapper<EntityTypeMobComponent> mEntityTypeMob;
    private ComponentMapper<UUIDComponent> mUUID;
    private ComponentMapper<PlayerComponent> mPlayers;
    private ComponentMapper<MovingComponent> mMoving;
    private ComponentMapper<HeadingComponent> mHeading;

    private ChunkTrackerSystem chunkTrackerSystem;

    @Override
    protected boolean checkProcessing() {
        return false;
    }


    @Override
    protected void processSystem() {

    }

    /**
     * A new player or mab has been added
     *
     * @param entityId the new entity
     */
    @Override
    protected void inserted(int entityId) {
        LocatedComponent locatedComponent = mLocated.get(entityId);
        boolean hasGravity = mGravity.has(entityId);

        // Send the spawning packet
        chunkTrackerSystem.broadcastTo(locatedComponent.getChunkX(), locatedComponent.getChunkZ(),
                createSpawnPacket(entityId), entityId
        );
        ServerEntityMetadataPacket metadataPacket = new ServerEntityMetadataPacket(entityId, new EntityMetadata[]{
//                new EntityMetadata(0, MetadataType.BYTE, 0 ),

                new EntityMetadata(2, MetadataType.OPTIONAL_CHAT, Component.text("Hello World")),
                new EntityMetadata(3, MetadataType.BOOLEAN, true),
//                new EntityMetadata(5, MetadataType.BOOLEAN, !hasGravity),
                new EntityMetadata(15, MetadataType.BOOLEAN, true),
//                new EntityMetadata(12, MetadataType.OPTIONAL_VARINT, true),


        });
        chunkTrackerSystem.broadcastTo(locatedComponent.getChunkX(), locatedComponent.getChunkZ(),
                metadataPacket, entityId
        );
    }

    @Override
    protected void removed(int entityId) {
        LocatedComponent locatedComponent = mLocated.get(entityId);
        chunkTrackerSystem.broadcastTo(locatedComponent.getChunkX(), locatedComponent.getChunkZ(),
                new ServerRemoveEntityPacket(entityId)
        );
    }

    private Packet createSpawnPacket(int eid) {
        if (mEntityTypeMob.has(eid)) {
            return createMobSpawnPacket(eid,
                    mUUID.get(eid), mEntityTypeMob.get(eid),
                    mLocated.get(eid),
                    mMoving.get(eid),
                    mHeading.get(eid));
        } else {
            return createPlayerSpawnPacket(eid, mUUID.get(eid), mPlayers.get(eid), mLocated.get(eid), mHeading.get(eid));
        }
    }

    private ServerSpawnLivingEntityPacket createMobSpawnPacket(int eid, UUIDComponent uuidComponent,
                                                               EntityTypeMobComponent typeMobComponent,
                                                               LocatedComponent locatedComponent,
                                                               MovingComponent movingComponent,
                                                               HeadingComponent headingComponent) {
        //TODO: Living vs non living
        return new ServerSpawnLivingEntityPacket(
                eid,
                uuidComponent.getUuid(),
                typeMobComponent.getMobType(),
                locatedComponent.getLocation().getX(),
                locatedComponent.getLocation().getY(),
                locatedComponent.getLocation().getZ(),
                headingComponent != null ? headingComponent.getYaw() : 0,
                headingComponent != null ? headingComponent.getPitch() : 0,
                0, //TODO: headYaw
                movingComponent != null ? movingComponent.getMotX() : 0,
                movingComponent != null ? movingComponent.getMotY() : 0,
                movingComponent != null ? movingComponent.getMotZ() : 0
//            ,
//            new EntityMetadata[0]
        );
    }

    private Packet createPlayerSpawnPacket(int eid, UUIDComponent uuidComponent,
                                           PlayerComponent playerComponent,
                                           LocatedComponent locatedComponent,
                                           HeadingComponent headingComponent) {
        return new ServerSpawnPlayerPacket(eid, uuidComponent.getUuid(), locatedComponent.getLocation().getX(),
                locatedComponent.getLocation().getY(),
                locatedComponent.getLocation().getZ(),
                headingComponent != null ? headingComponent.getYaw() : 0,
                headingComponent != null ? headingComponent.getPitch() : 0
//            ,new EntityMetadata[0]
        );
    }

    @Handler
    public void onTrack(PlayerTrackChunkEvent event) {
        chunkTrackerSystem.getEntities(event.getChunk().getX(), event.getChunk().getZ())
                .stream()
                .filter(x -> mEntityTypeMob.has(x) || mPlayers.has(x))
                .filter(x -> x != event.getPlayer().getId())
                .filter(mLocated::has)
                .filter(mUUID::has).forEach(eid -> {

            mPlayers.get(event.getPlayer()).getSession().send(createSpawnPacket(eid));
        });
    }

    @Handler
    public void onUntrack(PlayerUnTrackChunkEvent event) {
        int[] entities = chunkTrackerSystem.getEntities(event.getChunkX(), event.getChunkZ())
                .stream()
                .filter(mEntityTypeMob::has)
                .mapToInt(v -> v)
                .toArray();
        if (entities.length == 0) {
            return;
        }

        Session s = mPlayers.get(event.getPlayer()).getSession();
        for (int i : entities)
            s.send(new ServerRemoveEntityPacket(i));
    }

    @Handler
    public void onChangeChunk(EntityChangeChunkEvent event) {
        if (!mEntityTypeMob.has(event.getEntity()) && !mPlayers.has(event.getEntity())) {
            return;
        }

        Packet spawnPacket = createSpawnPacket(event.getEntity().getId());

        ServerRemoveEntityPacket despawnPacket = new ServerRemoveEntityPacket(event.getEntity().getId());

        // Send despawn or spawn chunk depending on who is tracking
        chunkTrackerSystem.getDifferenceInTrackingPlayers(event.getFromChunkX(), event.getFromChunkZ(),
                event.getToChunkX(), event.getToChunkZ(),
                pid -> {
                    mPlayers.get(pid).getSession().send(despawnPacket);
                }, pid -> {
                    mPlayers.get(pid).getSession().send(spawnPacket);
                });
    }
}
