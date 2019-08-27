package es.luepg.ecs.event.entity;

import com.artemis.Entity;
import es.luepg.ecs.world.World;
import lombok.Getter;

/**
 * A player no longer tracks a chunk
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
public class PlayerUnTrackChunkEvent extends PlayerEvent {

    @Getter
    private final int chunkX, chunkZ;

    public PlayerUnTrackChunkEvent(Entity player, World world, int chunkX, int chunkZ) {
        super(player, world);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
}
