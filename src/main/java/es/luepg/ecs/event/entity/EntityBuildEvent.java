package es.luepg.ecs.event.entity;

import com.artemis.Component;
import com.artemis.utils.EntityBuilder;
import es.luepg.ecs.world.World;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * An event that a new entity is being created.
 * <p>
 * Active systems can use this to add their component
 *
 * @author elmexl
 * Created on 01.06.2019.
 */
@RequiredArgsConstructor
public class EntityBuildEvent<T extends EntityBuildEvent<?>> {
  @Getter
  private final World world;


  private final Map<Class<? extends Component>, Component> components = new HashMap<>();

  public T with(Component component) {
    this.components.put(component.getClass(), component);
    return (T) this;
  }

    public Component getC(Class<? extends Component> comp) {
        return this.components.get(comp);
    }

    /**
     * Build the {@link EntityBuilder}
     * <p>
     * There SHOULD be NO need to call this if you did not call the event
     *
     * @return the entity builder
     */
    public EntityBuilder buildBuilder() {
        EntityBuilder entityBuilder = new EntityBuilder(this.getWorld().getArtemisWorld());

        this.components.values().forEach(entityBuilder::with);

        return entityBuilder;
    }
}
