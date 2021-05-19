package es.luepg.ecs.world;

import com.artemis.BaseSystem;
import com.artemis.Entity;
import com.artemis.WorldConfiguration;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.opennbt.tag.builtin.*;
import es.luepg.ecs.event.world.PreWorldCreateEvent;
import es.luepg.ecs.server.Server;
import es.luepg.ecs.timings.TimingHandler;
import es.luepg.ecs.timings.TimingInvocationStrategy;
import es.luepg.ecs.world.generator.CheckerWaveWorldGenerator;
import es.luepg.ecs.world.systems.*;
import es.luepg.ecs.world.systems.blocks.SimpleBlockSystem;
import es.luepg.ecs.world.systems.inventory.PlayerInventorySystem;
import es.luepg.ecs.world.systems.movement.GravitySystem;
import es.luepg.ecs.world.systems.movement.MovementSystem;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.File;
import java.util.Map;
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
    @ToString.Exclude
    private CheckerWaveWorldGenerator worldGenerator = new CheckerWaveWorldGenerator();

    // World info - TODO: Move me to an event?
    // or load me from a config & use onJoin events
    @Getter
    private GameMode defaultGameMode = GameMode.CREATIVE;

//    @Getter
//    private WorldType worldType = WorldType.CUSTOMIZED;

    /**
     * The view distance that the client receives.
     * May be used by chunk handling systems
     */
    @Getter
    private final int viewDistance = 9;

    @Getter
    private final int height = 16 * 16;

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

        this.chunkProvider = new ChunkProvider(this);

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

//        worldConfiguration.register("")

        this.server.getEventBus().publish(new PreWorldCreateEvent(this, worldConfiguration));

        this.artemisWorld = new com.artemis.World(worldConfiguration);


        for (BaseSystem system : this.artemisWorld.getSystems()) {
            System.out.println("Subscribing " + system.getClass());
            this.server.getEventBus().subscribe(system);
        }

    }

    public CompoundTag getDimensionCodecTag() {
        CompoundTag tag = new CompoundTag("");

        CompoundTag dimensionTypes = new CompoundTag("minecraft:dimension_type");
        dimensionTypes.put(new StringTag("type", "minecraft:dimension_type"));
        ListTag dimensionTag = new ListTag("value");
        CompoundTag overworldTag = convertToValue(getName(), 0, getDimensionTag().getValue());
        dimensionTag.add(overworldTag);
        dimensionTypes.put(dimensionTag);
        tag.put(dimensionTypes);

        CompoundTag biomeTypes = new CompoundTag("minecraft:worldgen/biome");
        biomeTypes.put(new StringTag("type", "minecraft:worldgen/biome"));
        ListTag biomeTag = new ListTag("value");
        CompoundTag plainsTag = convertToValue("minecraft:plains", 0, getPlainsTag().getValue());
        biomeTag.add(plainsTag);
        biomeTypes.put(biomeTag);
        tag.put(biomeTypes);

        return tag;
    }


    public CompoundTag getDimensionTag() {
        CompoundTag overworldTag = new CompoundTag("");
        overworldTag.put(new StringTag("name", getName()));
        overworldTag.put(new ByteTag("piglin_safe", (byte) 0));
        overworldTag.put(new ByteTag("natural", (byte) 1));
        overworldTag.put(new FloatTag("ambient_light", 0f));
        overworldTag.put(new StringTag("infiniburn", "minecraft:infiniburn_overworld"));
        overworldTag.put(new ByteTag("respawn_anchor_works", (byte) 0));
        overworldTag.put(new ByteTag("has_skylight", (byte) 1));
        overworldTag.put(new ByteTag("bed_works", (byte) 1));
        overworldTag.put(new StringTag("effects", "minecraft:overworld"));
        overworldTag.put(new ByteTag("has_raids", (byte) 1));
        overworldTag.put(new IntTag("logical_height", 256));
        overworldTag.put(new FloatTag("coordinate_scale", 1f));
        overworldTag.put(new ByteTag("ultrawarm", (byte) 0));
        overworldTag.put(new ByteTag("has_ceiling", (byte) 0));
        overworldTag.put(new IntTag("height", this.getHeight()));
        overworldTag.put(new IntTag("min_y", (int) 0));

        return overworldTag;
    }

    private static CompoundTag getPlainsTag() {
        CompoundTag plainsTag = new CompoundTag("");
        plainsTag.put(new StringTag("name", "minecraft:plains"));
        plainsTag.put(new StringTag("precipitation", "rain"));
        plainsTag.put(new FloatTag("depth", 0.125f));
        plainsTag.put(new FloatTag("temperature", 0.8f));
        plainsTag.put(new FloatTag("scale", 0.05f));
        plainsTag.put(new FloatTag("downfall", 0.4f));
        plainsTag.put(new StringTag("category", "plains"));

        CompoundTag effects = new CompoundTag("effects");
        effects.put(new LongTag("sky_color", 7907327));
        effects.put(new LongTag("water_fog_color", 329011));
        effects.put(new LongTag("fog_color", 12638463));
        effects.put(new LongTag("water_color", 4159204));

        CompoundTag moodSound = new CompoundTag("mood_sound");
        moodSound.put(new IntTag("tick_delay", 6000));
        moodSound.put(new FloatTag("offset", 2.0f));
        moodSound.put(new StringTag("sound", "minecraft:ambient.cave"));
        moodSound.put(new IntTag("block_search_extent", 8));

        effects.put(moodSound);

        plainsTag.put(effects);

        return plainsTag;
    }

    private static CompoundTag convertToValue(String name, int id, Map<String, Tag> values) {
        CompoundTag tag = new CompoundTag(name);
        tag.put(new StringTag("name", name));
        tag.put(new IntTag("id", id));
        CompoundTag element = new CompoundTag("element");
        element.setValue(values);
        tag.put(element);

        return tag;
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
