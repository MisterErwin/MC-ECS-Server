package es.luepg.ecs.event.login;

import com.artemis.Entity;
import es.luepg.ecs.event.entity.PlayerEvent;
import es.luepg.ecs.world.World;

/**
 * The player has joined and has been spawned in the world
 *
 * @author elmexl
 * Created on 27.07.2019.
 */
public class PlayerJoinedAndSpawnedInWorldEvent extends PlayerEvent {
    public PlayerJoinedAndSpawnedInWorldEvent(Entity player, World world) {
        super(player, world);
    }


}
