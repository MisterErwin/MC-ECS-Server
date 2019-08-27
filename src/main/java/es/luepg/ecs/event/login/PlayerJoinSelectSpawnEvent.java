package es.luepg.ecs.event.login;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.Session;
import es.luepg.ecs.world.util.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Mostly used to select the world for the player join
 * <p>
 * Be aware this packet is published when the world is not yet loaded and thus eventhandler NOT registered
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
@AllArgsConstructor
public class PlayerJoinSelectSpawnEvent {
    @Getter
    private final Session session;

    @Getter
    private final GameProfile gameProfile;

    /**
     * WARNING: The world may not be loaded at this time
     */
    @Getter
    @Setter
    private String worldName;

    @Getter
    @Setter
    private Location spawnLocation;
}
