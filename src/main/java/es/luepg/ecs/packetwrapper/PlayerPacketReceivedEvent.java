package es.luepg.ecs.packetwrapper;

import com.artemis.Entity;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import es.luepg.ecs.world.World;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;

/**
 * An event that a packet has been received
 * The session and packet itself are never null
 *
 * @author elmexl
 * Created on 21.07.2019.
 */
@Getter
@AllArgsConstructor
@ToString
public abstract class PlayerPacketReceivedEvent<T extends Packet> {
    @ToString.Exclude
    private final Session session;
    private final T packet;

    /**
     * The world the player currently resided in.
     * May be null if:
     * - the GameProfile is null
     * - a world has not yet been joined/set
     */
    @Nullable
    private final World world;

    /**
     * The player artemis entity.
     * This may be null, if:
     * - the GameProfile is null
     * - the World is null
     * - the World has not yet ticked after a player joined it
     */
    @Nullable
    private final Entity playerEntity;

    /**
     * The GameProfile of that player.
     * May be null if the gameprofile has not already been setup
     * (before the LoginSuccessPacket has been send)
     */
    @Nullable
    private final GameProfile gameProfile;
}
