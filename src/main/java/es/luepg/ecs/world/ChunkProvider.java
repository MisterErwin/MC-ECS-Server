package es.luepg.ecs.world;

import com.artemis.BaseSystem;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.Palette;
import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import es.luepg.ecs.data.game.world.block.BlockStateRegistry;
import es.luepg.ecs.data.game.world.block.state.IBlockState;
import es.luepg.ecs.world.generator.CheckerWaveWorldGenerator;
import es.luepg.ecs.world.generator.IChunkColumnGenerator;
import es.luepg.mcdata.data.game.world.block.Blocks;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;

/**
 *
 * @author elmexl
 * Created on 18.07.2019.
 */
@RequiredArgsConstructor
public class ChunkProvider extends BaseSystem {

    private final LongObjectMap<Column> loadedChunkColumns = new LongObjectHashMap<>();

    private final IChunkColumnGenerator chunkColumnGenerator = new CheckerWaveWorldGenerator();

    @Nonnull
    private final World world;

    @Getter(lazy = true)
    private final File tempWorldFile = prepareTempWorldFile();

    private File prepareTempWorldFile() {
        return new File("chunks_" + world.getName().replace(":", "_"));
    }

    public IBlockState getBlockState(int x, int y, int z) {
        if (y < 0 || y >= world.getHeight()) {
            return Blocks.AIR.getDefaultState();
        }

        Column column = this.provideColumnAtBlockCoords(x, z);


        int blockStateID = column.getChunks()[y >> 4].get(x & 0b1111, y & 0b1111, z & 0b1111);
        return BlockStateRegistry.INSTANCE.getRegistryEntryByIndex(blockStateID);
    }

    public Palette getPalette(int x, int y, int z) {
        if (y < 0 || y >= world.getHeight()) {
            return null;
        }

        Column column = this.provideColumnAtBlockCoords(x, z);


        return column.getChunks()[y >> 4].getPalette();
    }

    public void setBlockState(int x, int y, int z, IBlockState blockState) {
        if (y < 0 || y >= world.getHeight()) {
            return;
        }

        Column column = this.provideColumnAtBlockCoords(x, z);

        column.getChunks()[y >> 4].set(x & 0b1111, y & 0b1111, z & 0b1111, blockState.getGlobalPaletteIndex());
    }

    public void saveColumn(int x, int z) throws IOException {
        Column column = this.provideColumnAtBlockCoords(x, z);
        this.saveChunk(column);
    }

    public Column provideColumnAtBlockCoords(int x, int z) {
        return provideColumnAtChunkCoords(x >> 4, z >> 4);
    }

    private File getChunkFile(int chunkX, int chunkZ) {
        if (!this.getTempWorldFile().exists())
            this.getTempWorldFile().mkdirs();
        return new File(this.getTempWorldFile(), chunkX + "_" + chunkZ + ".chunk");
    }

    private Column loadChunkFromFile(int chunkX, int chunkZ) {
        if (!this.getTempWorldFile().exists())
            this.getTempWorldFile().mkdirs();
        File chunkFile = this.getChunkFile(chunkX, chunkZ);
        if (!chunkFile.exists())
            return null;

        try (InputStream inputStream = new FileInputStream(chunkFile)) {

            NetInput dataIn = new StreamNetInput(inputStream);
            int x = dataIn.readInt();
            int z = dataIn.readInt();
            Chunk[] chunks = new Chunk[dataIn.readInt()];
            for (int i = 0; i < chunks.length; i++) {
                chunks[i] = Chunk.read(dataIn);
            }
            CompoundTag[] tileEntities = new CompoundTag[dataIn.readInt()];
            for (int i = 0; i < tileEntities.length; i++) {
                tileEntities[i] = (CompoundTag) NBTIO.readTag(inputStream);
            }
            CompoundTag heightMaps = (CompoundTag) NBTIO.readTag(inputStream);
            int[] biomeData = dataIn.readInts(dataIn.readInt());

            return new Column(x, z, chunks, tileEntities, heightMaps, biomeData);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveChunk(Column column) throws IOException {
        File chunkFile = this.getChunkFile(column.getX(), column.getZ());

        if (!chunkFile.exists())
            chunkFile.createNewFile();

        ByteArrayOutputStream dataBytes = new ByteArrayOutputStream();
        NetOutput dataOut = new StreamNetOutput(dataBytes);

        dataOut.writeInt(column.getX());
        dataOut.writeInt(column.getZ());

        dataOut.writeInt(column.getChunks().length);
        for (Chunk chunk : column.getChunks()) {
            Chunk.write(dataOut, chunk);
        }

        dataOut.writeInt(column.getTileEntities().length);
        for (CompoundTag tileEntity : column.getTileEntities()) {
            NBTIO.writeTag(dataBytes, tileEntity);
        }

        NBTIO.writeTag(dataBytes, column.getHeightMaps());

        dataOut.writeInt(column.getBiomeData().length);
        dataOut.writeInts(column.getBiomeData());

        try (OutputStream outputStream = new FileOutputStream(chunkFile, false)) {
            dataBytes.writeTo(outputStream);
        }

    }

    public Column provideColumnAtChunkCoords(int chunkX, int chunkZ) {
        Column column = getLoadedColumnAtChunkCoords(chunkX, chunkZ);

        // Load from disk
        if (column == null) {
            column = this.loadChunkFromFile(chunkX, chunkZ);
            if (column != null) {
                this.loadedChunkColumns.put(posToLong(chunkX, chunkZ), column);
            }
        }

        // Generate
        if (column == null) {
            column = this.chunkColumnGenerator.generateColumn(chunkX, chunkZ, this.world);
            this.loadedChunkColumns.put(posToLong(chunkX, chunkZ), column);

            // ToDo: GeneratedEvent
        }

        return column;
    }

    public void doUnloadChunk(int chunkX, int chunkZ) {
        this.loadedChunkColumns.remove(posToLong(chunkX, chunkZ));
    }

    private Column getLoadedColumnAtBlockCoords(int x, int z) {
        return getLoadedColumnAtChunkCoords(x >> 4, z >> 4);
    }

    @Nullable
    private Column getLoadedColumnAtChunkCoords(int chunkX, int chunkZ) {
        return this.loadedChunkColumns.get(posToLong(chunkX, chunkZ));
    }


    private static long posToLong(int chunkX, int chunkZ) {
        return (long) chunkX & 4294967295L | ((long) chunkZ & 4294967295L) << 32;
    }

    @Override
    protected void processSystem() {

    }

    @Override
    protected boolean checkProcessing() {
        return false;
    }
}
