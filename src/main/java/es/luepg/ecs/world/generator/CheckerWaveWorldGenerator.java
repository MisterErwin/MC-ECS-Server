package es.luepg.ecs.world.generator;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import es.luepg.ecs.data.game.world.block.state.IBlockState;
import es.luepg.ecs.world.World;
import es.luepg.mcdata.data.game.world.block.Blocks;
import lombok.NonNull;

/**
 * A WorldGenerator that just makes sure I don't fall into the void all the time
 * by generating a world in a checkerbox pattern
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
public class CheckerWaveWorldGenerator implements IChunkColumnGenerator {

    private final int flat_level = 5;

    @Override
    public Column generateColumn(int chunkX, int chunkZ, World world) {
        com.github.steveice10.mc.protocol.data.game.chunk.Chunk[] chunks = new Chunk[world.getHeight() / 16];
        for (int y = 0; y < world.getHeight() / 16; y++) {
            chunks[y] = new Chunk();
            chunks[y].set(0, 0, 0, 0); // add air block state as id 0
        }

        Column c = new Column(chunkX, chunkZ, chunks, new CompoundTag[0],
                heightmaps(), oneBiome(world.getHeight(), 0));

        this.populateCheckerWaves(c);

        return c;

    }

    /**
     * Fill the biome array with one biome
     * @param worldHeight the world height
     * @param biomeId the biomes id
     * @return the biomeData array filled
     */
    private static int[] oneBiome(int worldHeight, int biomeId) {
        int biomeBlockCount = (worldHeight / 4) * (16 / 4) * (16 / 4);
        int[] biomeData = new int[biomeBlockCount];
        // The array is ordered by x then z then y, in 4×4×4 blocks.
        // The array is indexed by ((y >> 2) & 63) << 4 | ((z >> 2) & 3) << 2 | ((x >> 2) & 3).
        for (int i = 0; i < biomeBlockCount; i++)
            biomeData[i] = biomeId;
        return biomeData;
    }

    private static CompoundTag heightmaps() {
        CompoundTag tag = new CompoundTag("Heightmaps");
//        tag.put(new LongArrayTag("MOTION_BLOCKING", fill()));
//        tag.put(new LongArrayTag("MOTION_BLOCKING_NO_LEAVES", fill()));
//        tag.put(new LongArrayTag("OCEAN_FLOOR", fill()));
//        tag.put(new LongArrayTag("WORLD_SURFACE", fill()));
        return tag;
    }

    public void populateCheckerWaves(Column column) {
        IBlockState concrete = Blocks.YELLOW_CONCRETE.getDefaultState();

        if ((column.getX() % 2) == 0 || (column.getZ() % 2) == 0) {
            concrete = Blocks.WHITE_CONCRETE.getDefaultState();
        }

        if ((column.getX() == 8 || column.getX() == -8) && column.getZ() <= 8 && column.getZ() >= -8) {
            concrete = Blocks.RED_CONCRETE.getDefaultState();
        }

        if ((column.getZ() == 8 || column.getZ() == -8) && column.getX() <= 8 && column.getX() >= -8) {
            concrete = Blocks.RED_CONCRETE.getDefaultState();
        }

        // Just nice looking waves
        int maxY = 1 + Math.abs((int) (Math.sin(column.getX() / 10d + column.getZ() / 15d) * 20));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < maxY; y++) {
                    set(column, x, y, z, concrete);
                }
            }
        }
    }

    private void set(Column column, int rx, int y, int rz, @NonNull IBlockState blockState) {
        int chunk = y >> 4;
        int ry = y - chunk * 16;
        column.getChunks()[chunk].set(rx, ry, rz, blockState.getGlobalPaletteIndex());
    }
}
