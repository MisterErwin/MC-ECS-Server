package es.luepg.ecs.world.util;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author elmexl
 * Created on 31.05.2019.
 */
@AllArgsConstructor
@Getter
@ToString
public class Location {
    private final double x, y, z;


    public Location() {
        this(0, 0, 0);
    }

    public Location add(Location other) {
        return new Location(getX() + other.getX(), getY() + other.getY(), getZ() + other.getZ());
    }

    public Location add(double ox, double oy, double oz) {
        return new Location(getX() + ox, getY() + oy, getZ() + oz);
    }

    public Position toPos() {
        return new Position((int) x, (int) y, (int) z);
    }

    public double distanceSquared(Location newLoc) {
        return pow(this.getX() - newLoc.getX())
                + pow(this.getY() - newLoc.getY())
                + pow(this.getZ() - newLoc.getZ());
    }

    private double pow(double d) {
        return d * d;
    }
}
