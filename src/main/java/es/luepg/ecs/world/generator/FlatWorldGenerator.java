package es.luepg.ecs.world.generator;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.Blocks;
import com.github.steveice10.mc.protocol.data.game.world.block.state.IBlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import lombok.NonNull;

/**
 * A WorldGenerator that just makes sure I don't fall into the void all the time
 *
 * @author elmexl
 * Created on 31.05.2019.
 */
public class FlatWorldGenerator implements IChunkColumnGenerator {

    private final int flat_level = 5;

    @Override
    public Column generateColumn(int chunkX, int chunkZ) {
        com.github.steveice10.mc.protocol.data.game.chunk.Chunk[] chunks = new Chunk[16];
        for (int y = 0; y < 16; y++)
            chunks[y] = new Chunk(true);

        Column c = new Column(chunkX, chunkZ, chunks, voidBiome(), new CompoundTag[0],
                heightmaps());

        this.populateCheckerWaves(c);

        return c;

    }

    private static int[] voidBiome() {
        int[] biome = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                biome[z * 16 | x] = 1;
            }
        }
        return biome;
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

        if ((column.getX() % 2) == 0 || (column.getZ() % 2) == 0)
            concrete = Blocks.WHITE_CONCRETE.getDefaultState();

        if ((column.getX() == 8 || column.getX() == -8) && column.getZ() <= 8 && column.getZ() >= -8)
            concrete = Blocks.RED_CONCRETE.getDefaultState();

        if ((column.getZ() == 8 || column.getZ() == -8) && column.getX() <= 8 && column.getX() >= -8)
            concrete = Blocks.RED_CONCRETE.getDefaultState();

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
        column.getChunks()[chunk].getBlocks().set(rx, ry, rz, blockState);
    }
}
