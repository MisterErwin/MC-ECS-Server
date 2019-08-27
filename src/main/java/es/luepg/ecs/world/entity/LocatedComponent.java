package es.luepg.ecs.world.entity;

import com.artemis.Component;
import es.luepg.ecs.world.util.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The entity has a location
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LocatedComponent extends Component {
    @Getter
    private Location location;

    public LocatedComponent(Location location) {
        this.location = location;
        this.chunkX = ((int) location.getX()) >> 4;
        this.chunkZ = ((int) location.getZ()) >> 4;
    }

    public void setLocation(Location location) {
        this.location = location;
        this.chunkX = ((int) location.getX()) >> 4;
        this.chunkZ = ((int) location.getZ()) >> 4;
    }

    @Getter
    private int chunkX, chunkZ;
}
