package es.luepg.ecs.world.generator;

import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import es.luepg.ecs.world.World;

/**
 * @author elmexl
 * Created on 18.07.2019.
 */
public interface IChunkColumnGenerator {
  Column generateColumn(int chunkX, int chunkY, World world);
}
