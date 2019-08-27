package es.luepg.ecs.world.entity;

import com.artemis.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Gravity affects this entity
 *
 * @author elmexl
 * Created on 26.08.2019.
 */

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GravityAffectedComponent extends Component {
    private double g = 2;
}
