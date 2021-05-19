package es.luepg.ecs.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class AABB {
    @Getter
    @Setter
    private double minX, minY, minZ, maxX, maxY, maxZ;

    private boolean intersect(AABB other) {
        return (this.minX <= other.maxX && this.maxX >= other.minX) &&
                (this.minY <= other.maxY && this.maxY >= other.minY) &&
                (this.minZ <= other.maxZ && this.maxZ >= other.minZ);

    }
}
