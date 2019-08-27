package es.luepg.ecs.event.entity;

import com.artemis.Entity;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import es.luepg.ecs.world.World;
import lombok.Getter;

/**
 * A player tracks a chunk
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
public class PlayerTrackChunkEvent extends PlayerEvent {

    @Getter
    private final Column chunk;

    public PlayerTrackChunkEvent(Entity player, World world, Column chunk) {
        super(player, world);
        this.chunk = chunk;
    }
}
