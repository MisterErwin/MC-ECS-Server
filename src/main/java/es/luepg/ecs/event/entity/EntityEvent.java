package es.luepg.ecs.event.entity;

import com.artemis.Entity;
import es.luepg.ecs.world.World;
import lombok.Data;

/**
 * Super class for entity events
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Data
public abstract class EntityEvent {
    private final Entity entity;
    private final World world;
}
