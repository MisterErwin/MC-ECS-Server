package es.luepg.ecs.event.world;

import com.artemis.WorldConfiguration;
import es.luepg.ecs.world.World;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Handle world creation
 * <p>
 * Use this to add Systems to your (artemis) world
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
@AllArgsConstructor
public class PreWorldCreateEvent {
    @Getter
    private final World world;
    @Getter
    private final WorldConfiguration worldConfiguration;
}
