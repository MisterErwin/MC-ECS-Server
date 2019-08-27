package es.luepg.ecs.event.login;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.Session;
import es.luepg.ecs.event.entity.EntityBuildEvent;
import es.luepg.ecs.world.World;
import lombok.Getter;

/**
 * A special EntityBuildEvent for players
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
public class PlayerBuildEvent extends EntityBuildEvent<PlayerBuildEvent> {
    @Getter
    private final Session session;

    @Getter
    private final GameProfile gameProfile;

    public PlayerBuildEvent(World world, Session session, GameProfile gameProfile) {
        super(world);
        this.session = session;
        this.gameProfile = gameProfile;
    }
}
