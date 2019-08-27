package es.luepg.ecs.event.entity;

import com.artemis.Entity;
import es.luepg.ecs.world.World;

/**
 * A player is about to quick the server.
 * <p>
 * The connection may already be "dead", but the entity is still there
 *
 * @author elmexl
 * Created on 27.07.2019.
 */
public class PrePlayerQuitEvent extends PlayerEvent {
    public PrePlayerQuitEvent(Entity player, World world) {
        super(player, world);
    }
}
