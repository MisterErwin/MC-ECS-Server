package es.luepg.ecs.world;

import com.github.steveice10.mc.protocol.data.game.chunk.BlockStorage;

/**
 * @author elmexl
 * Created on 31.05.2019.
 */
public class Chunk extends com.github.steveice10.mc.protocol.data.game.chunk.Chunk {

    private final int y;

    public Chunk(boolean skylight, int y) {
        super(skylight);
        this.y = y;
    }

    public Chunk(BlockStorage blocks, int y) {
        super(blocks);
        this.y = y;
    }
}
