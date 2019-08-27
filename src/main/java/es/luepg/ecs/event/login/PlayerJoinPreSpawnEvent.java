package es.luepg.ecs.event.login;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.Session;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.util.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Called after {@link PlayerJoinSelectSpawnEvent} decided on the world
 * <p>
 * The world has been created at this point & event handlers are listening
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
@AllArgsConstructor
public class PlayerJoinPreSpawnEvent {
    @Getter
    private final Session session;

    @Getter
    private final GameProfile gameProfile;

    @Getter
    @Setter
    private World world;

    @Getter
    @Setter
    private Location spawnLocation;
}
