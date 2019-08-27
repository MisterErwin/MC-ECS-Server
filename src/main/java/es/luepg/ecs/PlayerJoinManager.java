package es.luepg.ecs;

import com.artemis.Entity;
import com.artemis.utils.EntityBuilder;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import es.luepg.ecs.event.entity.PlayerTrackChunkEvent;
import es.luepg.ecs.event.entity.PrePlayerQuitEvent;
import es.luepg.ecs.event.login.*;
import es.luepg.ecs.packetwrapper.event.ingame.client.ClientKeepAlivePacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.ClientSettingsPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerPositionPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerPositionRotationPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerRotationPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.world.ClientTeleportConfirmPacketReceivedEvent;
import es.luepg.ecs.server.Server;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.entity.*;
import es.luepg.ecs.world.util.Location;
import es.luepg.ecs.world.util.SessionUtils;
import lombok.RequiredArgsConstructor;
import net.engio.mbassy.bus.common.DeadMessage;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author elmexl
 * Created on 01.06.2019.
 */
@RequiredArgsConstructor
@Listener(references = References.Strong)
// Make sure this is a strong reference...
public class PlayerJoinManager {


    private final Server server;

    @Handler()
    public void onSessionAdd(SessionJoinEvent event) {
        System.out.println("New session joined");

        GameProfile gameProfile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);

        PlayerJoinSelectSpawnEvent chooseWorldEvent =
                new PlayerJoinSelectSpawnEvent(event.getSession(), gameProfile, "default_world", new Location(0, 63, 0));

        server.getEventBus().publish(chooseWorldEvent);

        System.out.println(gameProfile.getName() + " Joined to " + chooseWorldEvent.getWorldName());

        World world = server.getWorld(chooseWorldEvent.getWorldName());

        PlayerJoinPreSpawnEvent preSpawnEvent =
                new PlayerJoinPreSpawnEvent(event.getSession(), gameProfile, world, chooseWorldEvent.getSpawnLocation());

        server.getEventBus().publish(preSpawnEvent);
        // ToDo: Load this


        PlayerBuildEvent playerBuildEvent = new PlayerBuildEvent(world, event.getSession(), gameProfile);

        playerBuildEvent.with(new PlayerComponent(event.getSession(), gameProfile));
        playerBuildEvent.with(new UUIDComponent(gameProfile.getId()));
        playerBuildEvent.with(new LocatedComponent(preSpawnEvent.getSpawnLocation()));
        playerBuildEvent.with(new FlyableComponent(true, false));
        playerBuildEvent.with(new MovingComponent());
        playerBuildEvent.with(new HeadingComponent());

        server.getEventBus().publish(playerBuildEvent);

        EntityBuilder entityBuilder = playerBuildEvent.buildBuilder();
        Entity playerEntity = entityBuilder.build();

        System.out.println("Building playerEntity during JOIN ================ ");
        System.out.println(playerEntity.getComponent(PlayerComponent.class).getGameProfile().getId());
        System.out.println(playerEntity.getId());

        // SENd the packets last

        event.getSession().send(new ServerJoinGamePacket(playerEntity.getId(), false,
                world.getDefaultGamemode(), world.getDimensionId(), 10,
                world.getWorldType(), world.getViewDistance(), false));

        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            NetOutput brand = new StreamNetOutput(bout);
            brand.writeString("ecs");

            event.getSession().send(new ServerPluginMessagePacket("minecraft:brand", bout.toByteArray()));

            server.setWorldOfPlayer(gameProfile.getId(), world);
        } catch (IOException e) {
            e.printStackTrace();
        }

        event.getSession().send(new ServerSpawnPositionPacket(chooseWorldEvent.getSpawnLocation().toPos()));

        // TODO LOAD these from somewhere
        event.getSession().send(new ServerPlayerAbilitiesPacket(
                true,
                playerEntity.getComponent(FlyableComponent.class).isFlying(),
                playerEntity.getComponent(FlyableComponent.class).isCanFly(),
                true,
                0.2f,
                0.2f
        ));

        System.err.println("Sending pos & abilities");

        world.scheduleAfterTick(() -> {
            server.getEventBus().publish(new PlayerJoinedAndSpawnedInWorldEvent(playerEntity, world));
        });
    }

    @Handler
    public void onClientSettings(ClientSettingsPacketReceivedEvent event) {
        System.err.println("Handling client settings");

        World world = event.getWorld();

        System.out.println("queuing stuff for the next tick");
        world.scheduleAfterTick(() -> {
            System.err.println("Handling client stuff after the world had the chance of ticking ");
            System.out.println("Handling client stuff after the world had the chance of ticking ");

            Entity player = event.getPlayerEntity();

            LocatedComponent loc = player.getComponent(LocatedComponent.class);


            event.getSession().send(
                    new ServerPlayerPositionRotationPacket(loc.getLocation().getX(),
                            loc.getLocation().getY(), loc.getLocation().getZ(),
                            90, 0, 42));

        });
    }


    @Handler
    public void onSpawnFinish(ClientTeleportConfirmPacketReceivedEvent event) {
        if (((ClientTeleportConfirmPacket) event.getPacket()).getTeleportId() != 42)
            return;

        System.err.println("HANDLING ClientTeleportConfirmPacket 42");

        GameProfile playerProf = SessionUtils.getGameProfile(event.getSession());
        World world = server.getWorldOfPlayer(playerProf.getId());

        Entity player = world.getPlayerEntity(playerProf.getId());

        LocatedComponent loc = player.getComponent(LocatedComponent.class);

        int vd = world.getViewDistance() * 16;


        int ox = (int) loc.getLocation().getX();
        int oz = (int) loc.getLocation().getZ();

        for (int x = ox - vd; x < ox + vd; x += 16) {
            for (int z = oz - vd; z < oz + vd; z += 16) {
                Column c = world.getChunkProvider().provideColumnAtBlockCoords(x, z);

                event.getSession().send(
                        new ServerChunkDataPacket(c)
                );

                world.getServer().getEventBus().publish(new PlayerTrackChunkEvent(player, world, c));

            }
        }

    }

    @Handler
    public void onLeave(SessionRemovedEvent sessionRemovedEvent) {
        GameProfile playerProf = SessionUtils.getGameProfile(sessionRemovedEvent.getSession());
        World world = server.getWorldOfPlayer(playerProf.getId());

        Entity player = world.getPlayerEntity(playerProf.getId());

        // Send an event
        world.getServer().getEventBus().publish(new PrePlayerQuitEvent(player, world));

        System.out.println("Deleting entity object of " + playerProf.getName());
        world.getArtemisWorld().deleteEntity(player);
    }

    //TODO
    @Handler
    public void oNDead(DeadMessage deadMessage) {
        if (deadMessage.getMessage() instanceof ClientPlayerRotationPacketReceivedEvent
                || deadMessage.getMessage() instanceof ClientPlayerPositionPacketReceivedEvent
                || deadMessage.getMessage() instanceof ClientKeepAlivePacketReceivedEvent
                || deadMessage.getMessage() instanceof ClientPlayerPositionRotationPacketReceivedEvent
        )
            return;
        System.out.println("DIED:" + deadMessage.getMessage());
    }

}
