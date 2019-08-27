package es.luepg.ecs.world.entity;

import com.artemis.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The entity is able to fly
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
@AllArgsConstructor
@NoArgsConstructor
public class FlyableComponent extends Component {
    @Getter
    @Setter
    private boolean canFly;

    @Getter
    @Setter
    private boolean flying;

}
