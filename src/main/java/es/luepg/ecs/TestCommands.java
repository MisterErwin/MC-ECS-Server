package es.luepg.ecs;

import com.artemis.Entity;
import com.artemis.utils.EntityBuilder;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.packetlib.Session;
import es.luepg.ecs.event.entity.EntityBuildEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.ClientChatPacketReceivedEvent;
import es.luepg.ecs.server.Server;
import es.luepg.ecs.timings.TimingHandler;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.entity.*;
import es.luepg.ecs.world.systems.ChunkTrackerSystem;
import es.luepg.ecs.world.systems.PlayerEntityMapper;
import es.luepg.ecs.world.util.SessionUtils;
import lombok.RequiredArgsConstructor;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * This class is a temporary command handler
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Listener
@RequiredArgsConstructor
public class TestCommands {

    private final Server server;

    @Handler
    public void onChattest(ClientChatPacketReceivedEvent event) {
        if (event.getPacket().getMessage().startsWith("/"))
            return;
        GameProfile profile = SessionUtils.getGameProfile(event.getSession());

        Message msg = new TextMessage(profile.getName() + ": ");

        Message player_text = new TextMessage(event.getPacket().getMessage());
        msg.addExtra(player_text);

//        Message msg = new TextMessage("Hello, ").setStyle(new MessageStyle().setColor(ChatColor.GREEN));
//        Message name = new TextMessage(profile.getName()).setStyle(new MessageStyle().setColor(ChatColor.AQUA).addFormat(ChatFormat.UNDERLINED));
//        Message end = new TextMessage("!");
//        msg.addExtra(name);
//        msg.addExtra(end);

        World w = server.getWorldOfPlayer(profile.getId());
        w.getArtemisWorld().getSystem(PlayerEntityMapper.class).listPlayers()
                .forEach(
                        player -> {
                            player.getComponent(PlayerComponent.class)
                                    .getSession().send(new ServerChatPacket(msg));
                        }
                );

        System.out.println(msg.toString());

    }


    @Handler
    public void onChatCommand(ClientChatPacketReceivedEvent event) {
        if (!event.getPacket().getMessage().startsWith("/"))
            return;

        ClientChatPacket packet = event.getPacket();
        GameProfile profile = SessionUtils.getGameProfile(event.getSession());
        System.out.println(profile.getName() + ": " + packet.getMessage());

        if (packet.getMessage().startsWith("/sendchunk")) {
            String[] sp = packet.getMessage().split(" ", 3);
            if (sp.length != 3) {
                sendMessage(event.getSession(), "/sendchunk x z");
                return;
            }
            int x = Integer.parseInt(sp[1]);
            int z = Integer.parseInt(sp[2]);
            Column c = server.getWorldOfPlayer(profile.getId())
                    .getChunkProvider()
                    .provideColumnAtChunkCoords(x, z);
            event.getSession().send(new ServerChunkDataPacket(c));
            sendMessage(event.getSession(), "Send of chunk " + x + ",  " + z);
        } else if (packet.getMessage().startsWith("/getpos")) {
            World w = server.getWorldOfPlayer(profile.getId());
            LocatedComponent loc = w.getPlayerEntity(profile.getId()).getComponent(LocatedComponent.class);
            sendMessage(event.getSession(), "Located at: word=" + w.getName() + ", " + loc.getLocation().toString());

            sendMessage(event.getSession(), "chunkX=" + (((int) loc.getLocation().getX()) >> 4) + ",chunkZ=" + (((int) loc.getLocation().getZ()) >> 4));
            sendMessage(event.getSession(), Objects.toString(loc));
            sendMessage(event.getSession(), Objects.toString(w.getPlayerEntity(profile.getId()).getComponent(MovingComponent.class)));
        } else if (packet.getMessage().startsWith("/listentities")) {
            //List all entities in your chunk
            World w = server.getWorldOfPlayer(profile.getId());
            LocatedComponent loc = w.getPlayerEntity(profile.getId()).getComponent(LocatedComponent.class);

            sendMessage(event.getSession(),
                    w.getArtemisWorld().getSystem(ChunkTrackerSystem.class)
                            .getEntities(loc.getChunkX(), loc.getChunkZ()).toString());
        } else if (packet.getMessage().startsWith("/memory")) {
            // Show memory details - for if you are too lazy to use VisualVM
            NumberFormat f = new DecimalFormat("###,##0.0");
            sendMessage(event.getSession(), f.format(((double) Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0))
                    + "MB / " + f.format(Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0)) + "MB");

            if (packet.getMessage().equals("/memory gc")) {
                System.gc();
                sendMessage(event.getSession(), "GC ran");
            }

        } else if (packet.getMessage().startsWith("/timings start")) {
            // Enable timings
            World w = server.getWorldOfPlayer(profile.getId());
            w.getArtemisTimer().setEnableSystemTiming(true);
            sendMessage(event.getSession(), "Timings started");
        } else if (packet.getMessage().startsWith("/timings stop")) {
            // Disable timings
            World w = server.getWorldOfPlayer(profile.getId());
            w.getArtemisTimer().setEnableSystemTiming(false);
            sendMessage(event.getSession(), "Timings stopped");
        } else if (packet.getMessage().startsWith("/timings reset")) {
            // Reset timings
            World w = server.getWorldOfPlayer(profile.getId());
            w.getArtemisTimer().reset();
            TimingHandler.resetAll();
            sendMessage(event.getSession(), "Timings reset");
        } else if (packet.getMessage().startsWith("/timings print")) {
            // Print timings
            TimingHandler.print(System.out);
            System.out.println("-------- WORLD -------");
            World w = server.getWorldOfPlayer(profile.getId());
            w.getArtemisTimer().print(System.out);
        } else if (packet.getMessage().startsWith("/spawntest")) {
            // Spawn an entity
            System.out.println("-------- Testing to spawn -------");
            World w = server.getWorldOfPlayer(profile.getId());

            EntityBuildEvent entityBuildEvent = new EntityBuildEvent(w);

            // the new entity requires has an UUID
            entityBuildEvent.with(new UUIDComponent(UUID.randomUUID())); // ToDo: Make sure the UUID is really unique?

            // a location
            entityBuildEvent.with(new LocatedComponent(event.getPlayerEntity().getComponent(LocatedComponent.class).getLocation()));
            // is of type zombie
            entityBuildEvent.with(new EntityTypeMobComponent(MobType.ZOMBIE));

            // And slowly moves
            entityBuildEvent.with(new MovingComponent(0.1, 0, 0, null, 0));
            // With a gravity const acc of 0.1
            entityBuildEvent.with(new GravityAffectedComponent(0.1));

            // Allow other systems to add their components
            server.getEventBus().publish(entityBuildEvent);

            EntityBuilder entityBuilder = entityBuildEvent.buildBuilder();
            Entity newEntity = entityBuilder.build();

            System.out.println("New entity: " + newEntity.getId());
        } else {
            sendMessage(event.getSession(), "Unhandled command");
        }
    }

    private void sendMessage(Session session, String txt) {
        Message msg = new TextMessage(txt);
        session.send(new ServerChatPacket(msg));
    }


}
