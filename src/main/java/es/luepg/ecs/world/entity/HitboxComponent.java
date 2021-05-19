package es.luepg.ecs.world.entity;

import com.artemis.Component;
import es.luepg.ecs.data.AABB;
import es.luepg.ecs.world.util.Location;
import lombok.*;

import javax.annotation.Nullable;


/**
 * The entity has a hitbox
 *
 */

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class HitboxComponent extends Component {
    private double height;
    private double width;

    @Nullable
    private AABB aabb;

}
