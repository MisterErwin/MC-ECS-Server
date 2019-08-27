package es.luepg.ecs.world.entity;

import com.artemis.Component;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.Session;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The entity is a player
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
@NoArgsConstructor
@AllArgsConstructor
public class PlayerComponent extends Component {
    @Getter
    @Setter
    private Session session;

    @Getter
    @Setter
    private GameProfile gameProfile;
}
