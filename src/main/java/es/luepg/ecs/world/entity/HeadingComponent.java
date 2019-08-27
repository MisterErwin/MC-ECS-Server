package es.luepg.ecs.world.entity;

import com.artemis.Component;
import lombok.*;


/**
 * Tracks heading (yaw & pitch) of an entity
 *
 * @author elmexl
 * Created on 26.08.2019.
 */

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class HeadingComponent extends Component {
    private float yaw;
    private float pitch;

    private float lastYaw;
    private float lastPitch;
}
