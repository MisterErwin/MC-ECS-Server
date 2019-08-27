package es.luepg.ecs.event.entity;

import com.artemis.Entity;
import es.luepg.ecs.world.World;
import lombok.Getter;

/**
 * An entity crossed a chunk border
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Getter
public class EntityChangeChunkEvent extends EntityEvent {

    private final int fromChunkX, fromChunkZ;

    private final int toChunkX, toChunkZ;

    public EntityChangeChunkEvent(Entity entity, World world, int fromChunkX, int fromChunkZ, int toChunkX, int toChunkZ) {
        super(entity, world);
        this.fromChunkX = fromChunkX;
        this.fromChunkZ = fromChunkZ;
        this.toChunkX = toChunkX;
        this.toChunkZ = toChunkZ;
    }
}
