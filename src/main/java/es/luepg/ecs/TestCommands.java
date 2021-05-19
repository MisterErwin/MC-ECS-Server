package es.luepg.ecs;

import com.artemis.Entity;
import com.artemis.utils.EntityBuilder;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.Palette;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.packetlib.Session;
import es.luepg.ecs.data.game.world.block.state.IBlockState;
import es.luepg.ecs.event.entity.EntityBuildEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.ClientChatPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.event.ingame.client.player.ClientPlayerInteractEntityPacketReceivedEvent;
import es.luepg.ecs.server.Server;
import es.luepg.ecs.timings.TimingHandler;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.entity.*;
import es.luepg.ecs.world.systems.ChunkTrackerSystem;
import es.luepg.ecs.world.systems.PlayerEntityMapper;
import es.luepg.ecs.world.util.Location;
import es.luepg.ecs.world.util.SessionUtils;
import es.luepg.mcdata.data.game.world.Material;
import es.luepg.mcdata.data.game.world.block.Blocks;
import lombok.RequiredArgsConstructor;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.UUID;

import static com.artemis.Aspect.all;

/**
 * This class is a temporary command and event handler
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
@Listener
@RequiredArgsConstructor
public class TestCommands {

    private final Server server;

    @Handler
    public void onInteract(ClientPlayerInteractEntityPacketReceivedEvent event) {
        // Yell the position of entities
        Entity entity = event.getWorld().getArtemisWorld().getEntity(event.getPacket().getEntityId());

        LocatedComponent locatedComponent = entity.getComponent(LocatedComponent.class);
        sendMessage(event.getSession(), "Position: " + locatedComponent.toString());

        if (event.getPacket().getAction() == InteractAction.ATTACK)
            locatedComponent.setLocation(locatedComponent.getLocation().withY(Math.ceil(locatedComponent.getLocation().getY() + 2)));
    }

    @Handler
    public void onChattest(ClientChatPacketReceivedEvent event) {
        // Handle "chat"
        if (event.getPacket().getMessage().startsWith("/")) {
            return;
        }
        GameProfile profile = SessionUtils.getGameProfile(event.getSession());

        TextComponent msg = Component.text(profile.getName() + ": ")
                .append(Component.text(event.getPacket().getMessage()));

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
        if (!event.getPacket().getMessage().startsWith("/")) {
            return;
        }

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

            sendMessage(event.getSession(),
                    "chunkX=" + (((int) loc.getLocation().getX()) >> 4) + ",chunkZ=" + (((int) loc.getLocation()
                            .getZ()) >> 4));
            sendMessage(event.getSession(), Objects.toString(loc));
            sendMessage(event.getSession(),
                    Objects.toString(w.getPlayerEntity(profile.getId()).getComponent(MovingComponent.class)));
        } else if (packet.getMessage().startsWith("/give")) {
            int i = Integer.parseInt(packet.getMessage().split(" ")[1]);
            PlayerInventoryComponent component = event.getPlayerEntity().getComponent(PlayerInventoryComponent.class);
            Material m = Material.byItemProtocolID(i);
            if (m == null) {
                m = Material.AIR;
            }
            sendMessage(event.getSession(), "Item " + m.getName());
            component.getInventory()[36 + component.getSelectedHotBarIndex()] = m;

            ServerSetSlotPacket updateInventoryPacket = new ServerSetSlotPacket(0, 36, new ItemStack(m.getItemProtocolId()));
            event.getSession().send(updateInventoryPacket);
        } else if (packet.getMessage().startsWith("/save")) {
            World w = server.getWorldOfPlayer(profile.getId());
            LocatedComponent loc = w.getPlayerEntity(profile.getId()).getComponent(LocatedComponent.class);
            try {
                w.getChunkProvider().saveColumn((int) loc.getLocation().getX(), (int) loc.getLocation().getZ());
                sendMessage(event.getSession(), "Saved");
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage(event.getSession(), "Failed to save");
            }
        } else if (packet.getMessage().startsWith("/getblock")) {
            World w = server.getWorldOfPlayer(profile.getId());
            LocatedComponent loc = w.getPlayerEntity(profile.getId()).getComponent(LocatedComponent.class);

            IBlockState blockState = w.getChunkProvider()
                    .getBlockState((int) loc.getLocation().getX(), (int) loc.getLocation().getY() - 1,
                            (int) loc.getLocation().getZ());
            sendMessage(event.getSession(), blockState.getGlobalPaletteIndex() + ": " + blockState.getBlock().getName() + " ("
                    + (int) loc.getLocation().getX() + "|" + (int) loc.getLocation().getY() + "|" + (int) loc.getLocation()
                    .getZ() + ")");

            Palette palette = w.getChunkProvider()
                    .getPalette((int) loc.getLocation().getX(), (int) loc.getLocation().getY() - 1,
                            (int) loc.getLocation().getZ());

            System.out.println(palette.getClass());
            System.out.println(palette.size());
            for (int i = 0, l = palette.size(); i < l; i++)
                System.out.println(i + " " + palette.idToState(i));
            System.out.println();

        } else if (packet.getMessage().startsWith("/setblock")) {
            World w = server.getWorldOfPlayer(profile.getId());
            LocatedComponent loc = w.getPlayerEntity(profile.getId()).getComponent(LocatedComponent.class);

            w.getChunkProvider()
                    .setBlockState((int) loc.getLocation().getX(), (int) loc.getLocation().getY() - 1,
                            (int) loc.getLocation().getZ(), Blocks.STONE.getDefaultState());
            w.getArtemisWorld().getSystem(ChunkTrackerSystem.class)
                    .broadcastTo(
                            ((int) loc.getLocation().getX()) >> 4, ((int) loc.getLocation().getZ()) >> 4,
                            new ServerBlockChangePacket(
                                    new BlockChangeRecord(
                                            new Position((int) loc.getLocation().getX(), (int) loc.getLocation().getY(),
                                                    (int) loc.getLocation().getZ()),
                                            Blocks.STONE.getDefaultState().getGlobalPaletteIndex()
                                    )
                            )
                    );

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
            sendMessage(event.getSession(), f.format(
                    ((double) Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0))
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

            PrintStream ps = new PrintStream(new OutputStream() {
                private StringBuffer sb = new StringBuffer();

                @Override
                public void write(int b) {
                    if (b >= 32)
                        sb.append((char) b);
                }

                @Override
                public void flush() {
                    if (sb.length() == 0) return;
                    sendMessage(event.getSession(), sb.toString());
                    sb = new StringBuffer();
                }
            }, true);
            TimingHandler.print(System.out);
            TimingHandler.print(ps);
            System.out.println("-------- WORLD -------");
            ps.println("-------- WORLD -------");
            World w = server.getWorldOfPlayer(profile.getId());
            w.getArtemisTimer().print(System.out);
            w.getArtemisTimer().print(ps);
            System.out.println("---");
            ps.println("---");
            System.out.println("Entity count: " + w.getArtemisWorld().getAspectSubscriptionManager()
                    .get(all())
                    .getActiveEntityIds()
                    .cardinality()
            );
            ps.println("Entity count: " + w.getArtemisWorld().getAspectSubscriptionManager()
                    .get(all())
                    .getActiveEntityIds()
                    .cardinality()
            );
        } else if (packet.getMessage().startsWith("/spawntest")) {
            // Spawn an entity
            System.out.println("-------- Testing to spawn -------");
            World w = server.getWorldOfPlayer(profile.getId());

            Location loc = event.getPlayerEntity().getComponent(LocatedComponent.class).getLocation();
            for (int i = -20; i <= 20; i++)
                spawnZombie(w, loc.add(0, -3, i));

        } else {
            sendMessage(event.getSession(), "Unhandled command");
        }
    }

    private void spawnZombie(World world, Location location) {

        EntityBuildEvent<?> entityBuildEvent = new EntityBuildEvent<>(world);

        // the new entity requires has an UUID
        entityBuildEvent.with(new UUIDComponent(UUID.randomUUID())); // ToDo: Make sure the UUID is really unique?

        // a location
        entityBuildEvent
                .with(new LocatedComponent(location));
        // is of type zombie
        entityBuildEvent.with(new EntityTypeMobComponent(EntityType.ZOMBIE));

        // And slowly moves
        entityBuildEvent.with(new MovingComponent(0.1, 0, 0, null, 0));
        // With a gravity const acc of 0.1
        entityBuildEvent.with(new GravityAffectedComponent(0.1));

        // Allow other systems to add their components
        server.getEventBus().publish(entityBuildEvent);

        EntityBuilder entityBuilder = entityBuildEvent.buildBuilder();
        Entity newEntity = entityBuilder.build();
    }

    private void sendMessage(Session session, String txt) {
        TextComponent msg = Component.text(txt);
        session.send(new ServerChatPacket(msg));
    }


}
