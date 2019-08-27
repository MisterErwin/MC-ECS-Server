package es.luepg.ecs.world.entity;

import com.artemis.Component;
import es.luepg.ecs.world.util.Location;
import lombok.*;

import javax.annotation.Nullable;


/**
 * The entity is able to use velocity & movement packets should be send
 *
 * @author elmexl
 * Created on 26.08.2019.
 */

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class MovingComponent extends Component {
    private double motX;
    private double motY;
    private double motZ;

    @Nullable
    private Location lastLocation;

    /**
     * every 400 ticks a full teleport packet is sent, rather than just a relative movement, so that position remains fully synced
     */
    public int ticksSinceLastForcedTeleport;


    public int getAndIncreaseTicksSinceLastForcedTeleport() {
        return ticksSinceLastForcedTeleport++;
    }
}
