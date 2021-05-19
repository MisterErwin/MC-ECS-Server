package es.luepg.ecs.world.entity;

import com.artemis.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * The entity has an UUID
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
@NoArgsConstructor
@AllArgsConstructor
public class UUIDComponent extends Component {
    @Getter
    private UUID uuid;
}
