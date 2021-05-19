package es.luepg.ecs.server;

import com.artemis.Entity;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import es.luepg.ecs.PlayerJoinManager;
import es.luepg.ecs.TestCommands;
import es.luepg.ecs.event.login.SessionJoinEvent;
import es.luepg.ecs.packetwrapper.PlayerPacketReceivedEvent;
import es.luepg.ecs.packetwrapper.PlayerPacketReceivedEventHelper;
import es.luepg.ecs.world.World;
import es.luepg.ecs.world.util.SessionUtils;
import lombok.Getter;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.config.IBusConfiguration;
import net.kyori.adventure.text.Component;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The main server
 * Handles quite a few things
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
public class Server {

    private final Map<String, World> worlds = new HashMap<>();
    // Track the dimension IDs
    private final AtomicInteger dimensionWorldId = new AtomicInteger(0);

    // Track in which world a player plays
    private final Map<UUID, String> playerWorldMap = new HashMap<>();

    @Getter
    // The global eventbus
    private final MBassador<Object> eventBus;

    private final ServerSettings serverSettings;
    private com.github.steveice10.packetlib.Server packetServer;

    // We keep a local copy as otherwise we might be hit by the GC
    private final PlayerJoinManager joinRefernce;
    private final TestCommands testCommandsReference;

    public Server(ServerSettings settings) {
        this.serverSettings = settings;

        IBusConfiguration configuration = new BusConfiguration();


        configuration = configuration.addFeature(Feature.SyncPubSub.Default())
                .addFeature(Feature.AsynchronousHandlerInvocation.Default())
                .addFeature(Feature.AsynchronousMessageDispatch.Default())
        ;


        configuration = configuration.addPublicationErrorHandler(publicationError -> {
            System.err.println(publicationError);
            publicationError.getCause().printStackTrace();
        });


        this.eventBus = new MBassador<>(configuration);


        this.startServer();


        // Stuff

        this.eventBus.subscribe((joinRefernce = new PlayerJoinManager(this)));
        this.eventBus.subscribe((testCommandsReference = new TestCommands(this)));

    }

    /**
     * Load a new world
     *
     * @param name the name of the world
     * @return a newly created World
     */
    public World loadWorld(String name) {
        if (worlds.containsKey(name))
            throw new IllegalStateException("World " + name + " has already been loaded");
        System.out.println("Loading world " + name);

        World world = new World(name, dimensionWorldId.getAndIncrement(), this);

        this.worlds.put(name, world);

        world.startTicking();

        return world;
    }

    /**
     * Get a world by name (or load if not yet loaded)
     *
     * @param name the unique name of the world
     * @return the world
     */
    @Nonnull
    public World getWorld(String name) {
        World world = this.worlds.get(name);
        if (world == null) {
            world = loadWorld(name);
        }
        return world;
    }

    /**
     * Set the world in which a player is currently playing
     *
     * @param uuid  the players UUID
     * @param world the world the player just entered
     */
    public void setWorldOfPlayer(UUID uuid, World world) {
        this.playerWorldMap.put(uuid, world.getName());
    }

    /**
     * Get the world a player is currently playing in
     *
     * @param uuid the players UUID
     * @return the world
     */
    @Nonnull
    public World getWorldOfPlayer(UUID uuid) {
        String wn = this.playerWorldMap.get(uuid);
        if (wn == null) {
            throw new IllegalStateException("Player " + uuid + " is not tracked in any world");
        }
        return this.getWorld(wn);
    }


    private void startServer() {
        System.out.println("Starting with config: " + serverSettings.getHOST() + ":" + serverSettings.getPORT());

        // Start the packetlib Server
        this.packetServer = new com.github.steveice10.packetlib.Server(serverSettings.getHOST(), serverSettings.getPORT(),
                MinecraftProtocol.class, new TcpSessionFactory(serverSettings.getPROXY()));
//        packetServer.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, serverSettings.getAUTH_PROXY());
        packetServer.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, serverSettings.isVERIFY_USERS());
        // ToDo: Handle the server status info
        packetServer.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session -> new ServerStatusInfo(
                new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION),
                new PlayerInfo(100, 0, new GameProfile[0]), Component.text("Hello world!"),
                null));

        // Forward login to an event
        packetServer.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, (ServerLoginHandler) session -> {
            eventBus.publish(new SessionJoinEvent(session));
        });

        packetServer.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 100);

        packetServer.addListener(new ServerAdapter() {

            @Override
            public void serverClosed(ServerClosedEvent event) {
                eventBus.publish(event);
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
                eventBus.publish(event);

                event.getSession().addListener(new SessionAdapter() {
                    @Override
                    public void packetReceived(PacketReceivedEvent event) {

                        if (!(event.getPacket() instanceof ClientPlayerRotationPacket)
                                && !(event.getPacket() instanceof ClientPlayerPositionPacket)
                                && !(event.getPacket() instanceof ClientKeepAlivePacket)
                                && !(event.getPacket() instanceof ClientPlayerPositionRotationPacket)) {
                            System.out.println("on packet " + event.getPacket());
                        }

                        try {
                            GameProfile playerProf = SessionUtils.getGameProfile(event.getSession());
                            World world = null;
                            Entity player = null;

                            if (playerProf != null) {
                                world = getWorldOfPlayer(playerProf.getId());

                                if (world != null) {
                                    player = world.getPlayerEntity(playerProf.getId());
                                }
                            }


                            PlayerPacketReceivedEvent evt = PlayerPacketReceivedEventHelper.call(event.getSession(), event.getPacket(),
                                    world, player, playerProf);
                            if (evt != null) {
                                eventBus.publish(evt);
                            } else {
                                System.err.println("No handling of " + event.getPacket().getClass() + ":" + event.getPacket().toString());
                                eventBus.publish(event);
                            }
                        } catch (Exception e) {
                            System.err.println("Exception during handling of: " + event.getPacket().toString());
                            e.printStackTrace();
                        }
                    }
                });

            }

            @Override
            public void sessionRemoved(SessionRemovedEvent event) {
                eventBus.publish(event);
            }
        });

    }

    public void bind() {
        this.packetServer.bind();
    }
}
