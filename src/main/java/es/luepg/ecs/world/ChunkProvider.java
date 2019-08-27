package es.luepg.ecs.world;

import com.artemis.BaseSystem;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.Blocks;
import com.github.steveice10.mc.protocol.data.game.world.block.state.IBlockState;
import es.luepg.ecs.world.generator.FlatWorldGenerator;
import es.luepg.ecs.world.generator.IChunkColumnGenerator;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;

import javax.annotation.Nullable;

/**
 * @author elmexl
 * Created on 18.07.2019.
 */
public class ChunkProvider extends BaseSystem {

    private final LongObjectMap<Column> loadedChunkColumns = new LongObjectHashMap<>();

    private final IChunkColumnGenerator chunkColumnGenerator = new FlatWorldGenerator();

    private final static int MAX_BUILD_HEIGHT = 256;


    public IBlockState getBlockState(int x, int y, int z) {
        if (y < 0 || y >= MAX_BUILD_HEIGHT)
            return Blocks.AIR.getDefaultState();

        Column column = this.provideColumnAtBlockCoords(x, z);

        return column.getChunks()[y >> 4].getBlocks().get(x & 0b1111, y & 0b1111, z & 0b1111);
    }

    public void setBlockState(int x, int y, int z, IBlockState blockState) {
        if (y < 0 || y >= MAX_BUILD_HEIGHT)
            return;

        Column column = this.provideColumnAtBlockCoords(x, z);

        column.getChunks()[y >> 4].getBlocks().set(x & 0b1111, y & 0b1111, z & 0b1111, blockState);
    }

    public Column provideColumnAtBlockCoords(int x, int z) {
        return provideColumnAtChunkCoords(x >> 4, z >> 4);
    }


    public Column provideColumnAtChunkCoords(int chunkX, int chunkZ) {
        Column column = getLoadedColumnAtChunkCoords(chunkX, chunkZ);

        // ToDo: Load from disk

        if (column == null) {
            column = this.chunkColumnGenerator.generateColumn(chunkX, chunkZ);
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
