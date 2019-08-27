package es.luepg.ecs.world;

import com.artemis.BaseSystem;
import com.artemis.Entity;
import com.artemis.WorldConfiguration;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import es.luepg.ecs.event.world.PreWorldCreateEvent;
import es.luepg.ecs.server.Server;
import es.luepg.ecs.timings.TimingHandler;
import es.luepg.ecs.timings.TimingInvocationStrategy;
import es.luepg.ecs.world.generator.FlatWorldGenerator;
import es.luepg.ecs.world.systems.*;
import es.luepg.ecs.world.systems.blocks.SimpleBlockSystem;
import es.luepg.ecs.world.systems.inventory.PlayerInventorySystem;
import es.luepg.ecs.world.systems.movement.GravitySystem;
import es.luepg.ecs.world.systems.movement.MovementSystem;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author elmexl
 * Created on 31.05.2019.
 */
public class World {

    @Getter
    @NonNull
    private final String name;

    //ToDo: Serialize to here
    private final File worldFile;

    /**
     * An unique (to all loaded worlds) ID
     */
    @Getter
    private final int dimensionId;

    /**
     * The artemis world
     */
    @Getter
    private com.artemis.World artemisWorld;

    /**
     * Reference to the server
     */
    @Getter
    @NonNull
    private final Server server;

    // ToDo: Do this nicer
    @NonNull
    private FlatWorldGenerator worldGenerator = new FlatWorldGenerator();

    // World info - TODO: Move me to an event?
    // or load me from a config & use onJoin events
    @Getter
    private GameMode defaultGamemode = GameMode.CREATIVE;

    @Getter
    private WorldType worldType = WorldType.CUSTOMIZED;

    /**
     * The view distance that the client receives.
     * May be used by chunk handling systems
     */
    @Getter
    private int viewDistance = 9;

    /**
     * A queue holding tasks to run after the next world tick.
     * Might be useful for stuff that should happen AFTER a world change
     * <p>
     * ToDo: Check if we should lock this somehow during processing of this.
     * (Maybe with two queues that get switched all the time?)
     */
    private final Queue<Runnable> tasksToSyncAfterNextTick = new ConcurrentLinkedQueue<>();

    /**
     * Should this world be running
     */
    private boolean shouldBeRunning = false;
    /**
     * The worldTick Thread
     */
    private Thread runningThread = null;


    // Shortcut to the EntityMapper System
    private final PlayerEntityMapper playerEntityMapper = new PlayerEntityMapper();

    /**
     * The ChunkProvider of this world
     */
    @Getter
    private final ChunkProvider chunkProvider;


    /**
     * A custom {@link com.artemis.InvocationStrategy} that also times this
     */
    @Getter
    private TimingInvocationStrategy artemisTimer;

    /**
     * The TimingHandler that keeps track of the duration of a worldTick
     */
    private final TimingHandler worldTimer;

    public World(String name, int dimensionId, Server server) {
        this.name = name;
        this.dimensionId = dimensionId;
        this.server = server;

        this.worldFile = new File("worlds", this.name);

        this.chunkProvider = new ChunkProvider();

        this.worldTimer = new TimingHandler(getName() + ": Main Loop");

        this.initECS();
    }


    private void initECS() {
        WorldConfiguration worldConfiguration = new WorldConfiguration();

        worldConfiguration.setInvocationStrategy((artemisTimer = new TimingInvocationStrategy()));

        worldConfiguration.setSystem(new ReferenceSystem(this));

        worldConfiguration.setSystem(playerEntityMapper);
        worldConfiguration.setSystem(chunkProvider);

        worldConfiguration.setSystem(PlayerActiveMomentSystem.class); // Packet movement
        worldConfiguration.setSystem(PlayerSendWorldSystem.class); // Send chunks
        worldConfiguration.setSystem(ChunkTrackerSystem.class); // Track chunk subscriptions
        worldConfiguration.setSystem(EntityShowMobSystem.class); // Mob spawn & despawn packets
        worldConfiguration.setSystem(MovementSystem.class); // handle velocity moving

        worldConfiguration.setSystem(GravitySystem.class); // its going down...


        worldConfiguration.setSystem(SimpleBlockSystem.class); // Simple block having fun
        worldConfiguration.setSystem(PlayerInventorySystem.class); //Inventory system

        worldConfiguration.setSystem(PlayerListSystem.class); //Player list


        this.server.getEventBus().publish(new PreWorldCreateEvent(this, worldConfiguration));

        this.artemisWorld = new com.artemis.World(worldConfiguration);

        for (BaseSystem system : this.artemisWorld.getSystems()) {
            System.out.println("Subscribing " + system.getClass());
            this.server.getEventBus().subscribe(system);
        }

    }

    public Entity getPlayerEntity(UUID playerUUID) {
        return this.playerEntityMapper.getEntityForPlayer(playerUUID);
    }

    public void scheduleAfterTick(Runnable runnable) {
        this.tasksToSyncAfterNextTick.add(runnable);
    }

    // Before the world ticks, this might return false
    @Deprecated
    public boolean isPlayerKnown(UUID playerUUID) {
        return this.playerEntityMapper.getEntityIDForPlayer(playerUUID) != 0;
    }

    public void startTicking() {
        this.shouldBeRunning = true;

        runningThread = new Thread(new WorldRunner());
        runningThread.start();
    }

    public void initiateStop() {
        this.shouldBeRunning = false;
    }


    class WorldRunner implements Runnable {
        @Override
        public void run() {
            System.out.println(getName() + ": about to start ticking");
            try {
                long currentTime = System.currentTimeMillis();
                long i = 0;    //timeToTick
                this.tick();


                while (shouldBeRunning) {
                    long now = System.currentTimeMillis();
                    long diff = now - currentTime;

                    if (diff > 2000L) {
                        // TimeSinceLastwarning?
                        System.err.println("Can't keep up - yadayada a tick took" + diff + " ms");
                        diff = 2000L;
                    }

                    if (diff < 0L) {
                        System.err.println("Time ran backwards! Did the system time change?");
                        diff = 0L;
                    }
                    i += diff;
                    currentTime = now;

                    if (false) {
                        // Sleeping?
                    } else {
                        while (i > 50L) {
                            i -= 50L;
                            this.tick();
                        }
                    }
                    Thread.sleep(Math.max(1L, 50L - i));
                }
                System.out.println("No longer running");
            } catch (InterruptedException ioex) {
                ioex.printStackTrace();
            }
        }

        private void tick() {
            worldTimer.startTiming();
            artemisWorld.process();
            while (!tasksToSyncAfterNextTick.isEmpty()) {
                try {
                    tasksToSyncAfterNextTick.poll().run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            worldTimer.stopTiming();
        }
    }

    @Override
    public String toString() {
        return "World{" +
                "name='" + name + '\'' +
                ", dimensionId=" + dimensionId +
                ", shouldBeRunning=" + shouldBeRunning +
                '}';
    }

    @RequiredArgsConstructor
    public static class ReferenceSystem extends BaseSystem {
        @Getter
        private final World realWorld;

        @Override
        protected boolean checkProcessing() {
            return false;
        }

        @Override
        protected void processSystem() {
        }
    }
}
