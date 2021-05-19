package es.luepg.ecs.event.entity;

import com.artemis.Entity;
import es.luepg.ecs.world.World;
import lombok.Data;
import lombok.ToString;

/**
 * Super class for entity events
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Data
public abstract class EntityEvent {
    private final Entity entity;
  @ToString.Exclude
  private final World world;
}
