package es.luepg.ecs.event.entity;

import com.artemis.Entity;
import es.luepg.ecs.world.World;

/**
 * Super class for player events
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
public abstract class PlayerEvent extends EntityEvent {

    public PlayerEvent(Entity entity, World world) {
        super(entity, world);
    }

    public Entity getPlayer() {
        return getEntity();
    }
}
