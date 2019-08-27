package es.luepg.ecs.world.entity;

import com.artemis.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author elmexl
 * Created on 31.05.2019.
 */
@NoArgsConstructor
@AllArgsConstructor
public class TrackingChunksComponent extends Component {
    @Getter
    @Setter
    private Set<int[]> trackedChunks = new HashSet<>();

}
