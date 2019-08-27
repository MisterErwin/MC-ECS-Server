package es.luepg.ecs.world.systems.movement;

import com.artemis.ComponentMapper;
import com.artemis.annotations.All;
import com.artemis.systems.IteratingSystem;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.game.world.block.Blocks;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import es.luepg.ecs.world.ChunkProvider;
import es.luepg.ecs.world.entity.GravityAffectedComponent;
import es.luepg.ecs.world.entity.LocatedComponent;
import es.luepg.ecs.world.entity.MovingComponent;
import es.luepg.ecs.world.systems.ChunkTrackerSystem;
import es.luepg.ecs.world.util.Location;


/**
 * Apply gravity to entities
 *
 * @author elmexl
 * Created on 26.08.2019.
 */
@All({GravityAffectedComponent.class, MovingComponent.class, LocatedComponent.class})
public class GravitySystem extends IteratingSystem {

    private ComponentMapper<GravityAffectedComponent> mGravity;
    private ComponentMapper<MovingComponent> mMoving;
    private ComponentMapper<LocatedComponent> mLocated;
    private ChunkProvider chunkProvider;

    private ChunkTrackerSystem chunkTrackerSystem;

    @Override
    protected void process(int eid) {
        GravityAffectedComponent gravity = mGravity.get(eid);
        Location loc = mLocated.get(eid).getLocation();

        // Is the entity falling?
        //ToDo: Have a isOnGround
        if (chunkProvider.getBlockState((int) (loc.getX() + .5), (int) loc.getY() - 2, (int) (loc.getZ() + .5)).getBlock() != Blocks.AIR) {
            chunkTrackerSystem.broadcastTo(
                    (int) (loc.getX() + .5) >> 4, (int) (loc.getZ() + .5) >> 4,
                    new ServerBlockChangePacket(
                            new BlockChangeRecord(
                                    new Position((int) (loc.getX() + .5), (int) loc.getY() - 2, (int) (loc.getZ() + .5)),
                                    Blocks.GREEN_TERRACOTTA.getDefaultState()
                            )
                    )
            );
            return;
        }

        mMoving.get(eid).setMotY(mMoving.get(eid).getMotY() - gravity.getG());
    }
}
