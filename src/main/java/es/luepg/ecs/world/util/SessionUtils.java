package es.luepg.ecs.world.util;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.packetlib.Session;

/**
 * @author elmexl
 * Created on 18.07.2019.
 */
public class SessionUtils {
    public static GameProfile getGameProfile(Session session) {
        return session.getFlag(MinecraftConstants.PROFILE_KEY);
    }
}
