package es.luepg.ecs.world.entity;

import com.artemis.Component;
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import lombok.*;

/**
 * Declares an entity as a mob that should be rendered
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
@AllArgsConstructor
@NoArgsConstructor
public class EntityTypeMobComponent extends Component {
    @Getter
    @Setter
    @NonNull
    private MobType mobType;

}
