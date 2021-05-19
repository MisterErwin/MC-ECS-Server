package es.luepg.ecs.world.systems.movement;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import es.luepg.ecs.event.entity.EntityChangeChunkEvent;
import es.luepg.ecs.world.ChunkProvider;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.entity.HeadingComponent;
import es.luepg.ecs.world.entity.LocatedComponent;
import es.luepg.ecs.world.entity.MovingComponent;
import es.luepg.ecs.world.systems.ChunkTrackerSystem;
import es.luepg.ecs.world.util.Location;
import es.luepg.mcdata.data.game.world.block.Blocks;

/**
 * @author elmexl
 * Created on 26.08.2019.
 */
@All({MovingComponent.class, LocatedComponent.class})
public class MovementSystem extends IteratingSystem {

    private ComponentMapper<MovingComponent> mMoving;
    private ComponentMapper<LocatedComponent> mLocated;
    private ComponentMapper<HeadingComponent> mHeading;

    private ChunkProvider chunkProvider;

    private ChunkTrackerSystem trackerSystem;

    private final static float MOT_DAMPENING = 0.91f;

    private boolean collides(Location location) {
        return collides((int) Math.floor(location.getX() - 0.2), (int) Math.round(location.getY()),
                (int) Math.floor(location.getZ() - 0.2)) ||
                collides((int) Math.ceil(location.getX() + 0.2), (int) Math.round(location.getY()),
                        (int) Math.ceil(location.getZ() + 0.2));
    }

    private boolean collides(int x, int y, int z) {
        return chunkProvider.getBlockState(x, y, z).getBlock() != Blocks.AIR;
    }

    @Override
    protected void process(int eid) {
        MovingComponent moving = mMoving.get(eid);
        LocatedComponent locatedComponent = mLocated.get(eid);
        Location loc = locatedComponent.getLocation();

        // ToDo: Real collisions with es.luepg.ecs.data.AABB s
        if (moving.getMotX() != 0) {
            if (collides(loc.add(moving.getMotX(), 0, 0))) {
                moving.setMotX(0);
            } else {
                moving.setMotX(moving.getMotX() * MOT_DAMPENING);
            }
        }
        if (moving.getMotY() != 0) {
            if (collides(loc.add(0, moving.getMotY(), 0))) {
                moving.setMotY(0);
            } else {
                moving.setMotY(moving.getMotY() * MOT_DAMPENING);
            }
        }
        if (moving.getMotZ() != 0) {
            if (collides(loc.add(0, 0, moving.getMotZ()))) {
                moving.setMotZ(0);
            } else {
                moving.setMotZ(moving.getMotZ() * MOT_DAMPENING);
            }
        }

        int oldChunkX = locatedComponent.getChunkX();
        int oldChunkZ = locatedComponent.getChunkZ();

        Location newLoc = loc.add(moving.getMotX(), moving.getMotY(), moving.getMotZ());
        locatedComponent.setLocation(newLoc);

        if (oldChunkX != locatedComponent.getChunkX() || oldChunkZ != locatedComponent.getChunkZ()) {
            EntityChangeChunkEvent entityChangeChunkEvent = new EntityChangeChunkEvent(world.getEntity(eid), world.getSystem(
                    World.ReferenceSystem.class).getRealWorld(),
                    oldChunkX, oldChunkZ,
                    locatedComponent.getChunkX(),
                    locatedComponent.getChunkZ());
            world.getSystem(World.ReferenceSystem.class).getRealWorld().getServer().getEventBus()
                    .publish(entityChangeChunkEvent);
        }

        Location lastLoc = moving.getLastLocation();
        if (lastLoc == null) {
            lastLoc = loc;
        }

//        System.out.println("Moved " + eid + "  " + lastLoc.distanceSquared(newLoc) + " sqrt " + moving);

        moving.setLastLocation(newLoc);

        double dist = lastLoc.distanceSquared(newLoc);
        boolean rotationChanged = false;
        boolean yawChanged = false; // used to determine if we have to send Entity Head Look
        HeadingComponent hc = mHeading.get(eid);

        // handle heading
        if (hc != null) {
            yawChanged = hc.getLastYaw() != hc.getYaw();
            rotationChanged = yawChanged || hc.getLastPitch() != hc.getPitch();

            hc.setLastPitch(hc.getPitch());
            hc.setLastYaw(hc.getYaw());
        }

        if (dist == 0 && rotationChanged) {
            ServerEntityRotationPacket packet = new ServerEntityRotationPacket(eid,
                    hc.getYaw(),
                    hc.getPitch(),
                    true
            );
            trackerSystem.broadcastTo(locatedComponent.getChunkX(), locatedComponent.getChunkZ(), packet, eid);
        } else if (dist == 0 && !rotationChanged) {
            return;
        } else if (dist >= 8 * 8 || moving.getAndIncreaseTicksSinceLastForcedTeleport() > 400) {
            moving.setTicksSinceLastForcedTeleport(0);
            ServerEntityTeleportPacket teleportPacket = new ServerEntityTeleportPacket(
                    eid,
                    newLoc.getX(),
                    newLoc.getY(),
                    newLoc.getZ(),
                    hc == null ? 0 : hc.getYaw(), hc == null ? 0 : hc.getPitch(), false
            );
            trackerSystem.broadcastTo(locatedComponent.getChunkX(), locatedComponent.getChunkZ(), teleportPacket);
            System.out.println("Something moved > 8blocks/tick - might want to reset their location?");
            System.out.println(newLoc + "// " + dist);
            System.out.println("loc. " + loc);
            System.out.println("lastLoc " + lastLoc);
        } else if (!rotationChanged) {
            ServerEntityPositionPacket packet = new ServerEntityPositionPacket(eid,
                    newLoc.getX() - lastLoc.getX(),
                    newLoc.getY() - lastLoc.getY(),
                    newLoc.getZ() - lastLoc.getZ(), true);

            trackerSystem.broadcastTo(locatedComponent.getChunkX(), locatedComponent.getChunkZ(), packet, eid);
        } else {
            ServerEntityPositionRotationPacket packet = new ServerEntityPositionRotationPacket(eid,
                    newLoc.getX() - lastLoc.getX(),
                    newLoc.getY() - lastLoc.getY(),
                    newLoc.getZ() - lastLoc.getZ(),
                    hc.getYaw(), hc.getPitch(),
                    true);

            trackerSystem.broadcastTo(locatedComponent.getChunkX(), locatedComponent.getChunkZ(), packet, eid);
        }
        if (yawChanged) {
            trackerSystem.broadcastTo(locatedComponent.getChunkX(), locatedComponent.getChunkZ(),
                    new ServerEntityHeadLookPacket(eid, hc.getYaw()));
        }

    }
}
