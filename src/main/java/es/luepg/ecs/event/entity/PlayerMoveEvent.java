package es.luepg.ecs.event.entity;

import com.artemis.Entity;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.util.Location;
import lombok.Getter;

/**
 * A player moved / changed his position
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Getter
public class PlayerMoveEvent extends PlayerEvent {
    private final Location from;

    private final Location to;


    public PlayerMoveEvent(Entity player, World world, Location from, Location to) {
        super(player, world);
        this.from = from;
        this.to = to;
    }
}
