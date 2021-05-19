package es.luepg.ecs.world.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import es.luepg.ecs.event.entity.PrePlayerQuitEvent;
import es.luepg.ecs.event.login.PlayerJoinPreSpawnEvent;
import es.luepg.ecs.world.entity.PlayerComponent;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.kyori.adventure.text.Component;

import java.util.Collection;

/**
 * Provide a simple playerlist for players (TAB)
 *
 * @author elmexl
 * Created on 27.07.2019.
 */
@Listener
public class PlayerListSystem extends BaseSystem {

    private PlayerEntityMapper playerEntityMapper;
    private ComponentMapper<PlayerComponent> mPlayers;

    @Override
    protected void processSystem() {

    }

    @Override
    protected boolean checkProcessing() {
        return false;
    }

    @Handler
    public void onJoin(PlayerJoinPreSpawnEvent event) {
        PlayerEntityMapper playerEntityMapper = getWorld().getSystem(PlayerEntityMapper.class);
        Collection<Entity> players = playerEntityMapper.listPlayers();


        PlayerListEntry[] allPlayerEntries = new PlayerListEntry[players.size() + 1];
        PlayerListEntry[] newPlayerEntries = new PlayerListEntry[]{
                new PlayerListEntry(event.getGameProfile(), GameMode.SURVIVAL,
                        ((Number) event.getSession().getFlag(MinecraftConstants.PING_KEY)).intValue(),
                        Component.text(event.getGameProfile().getName())
                )

        };
        int i = 0;

        // The new player joined packed
        ServerPlayerListEntryPacket packet = new ServerPlayerListEntryPacket(PlayerListEntryAction.ADD_PLAYER,
                newPlayerEntries);

        for (Entity playerEntity : players) {
            PlayerComponent pc = mPlayers.get(playerEntity);
            allPlayerEntries[i] = new PlayerListEntry(pc.getGameProfile(),
                    GameMode.SURVIVAL,
                    ((Number) pc.getSession().getFlag(MinecraftConstants.PING_KEY))
                            .intValue(),
                    Component.text(pc.getGameProfile().getName() + " r " + playerEntity
                            .getId())
            );

            if (!pc.getGameProfile().equals(event.getGameProfile())) {
                pc.getSession().send(packet);
            } else {
                System.err.println("Not sending to myself#################################");
            }
        }
        allPlayerEntries[allPlayerEntries.length - 1] = newPlayerEntries[0];

        // Send all players to the new player
        packet = new ServerPlayerListEntryPacket(PlayerListEntryAction.ADD_PLAYER, allPlayerEntries);
        event.getSession().send(packet);
    }

    @Handler
    public void onLeave(PrePlayerQuitEvent event) {
        // Send a REMOVE_PLAYER to all players
        PlayerEntityMapper playerEntityMapper = getWorld().getSystem(PlayerEntityMapper.class);
        Collection<Entity> players = playerEntityMapper.listPlayers();
        players.remove(event.getPlayer());

        PlayerListEntry[] leavingPlayerEntries = new PlayerListEntry[]{
                new PlayerListEntry(mPlayers.get(event.getPlayer()).getGameProfile())
        };
        ServerPlayerListEntryPacket packet = new ServerPlayerListEntryPacket(PlayerListEntryAction.REMOVE_PLAYER,
                leavingPlayerEntries);

        for (Entity playerEntity : players) {
            PlayerComponent pc = mPlayers.get(playerEntity);
            pc.getSession().send(packet);
        }
    }
}
