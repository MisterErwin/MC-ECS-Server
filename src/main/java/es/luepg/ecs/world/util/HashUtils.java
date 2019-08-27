package es.luepg.ecs.world.util;

import com.github.steveice10.packetlib.Session;

import java.util.Objects;

/**
 * @author elmexl
 * Created on 18.07.2019.
 */
public class HashUtils {

    public static int getSessionHash(Session session) {
        return Objects.hash(session.getHost(), session.getPort());
    }
}
