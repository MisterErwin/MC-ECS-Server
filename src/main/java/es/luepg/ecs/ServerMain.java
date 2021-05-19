package es.luepg.ecs;

import es.luepg.ecs.server.Server;
import es.luepg.ecs.server.ServerSettings;
import es.luepg.mcdata.data.game.world.block.Blocks;

/**
 * @author elmexl
 * Created on 31.05.2019.
 */
public class ServerMain {

    public static void main(String[] a) throws Exception {

        System.out.println("Initializing blocks");
        long s = System.currentTimeMillis();
        if (Blocks.AIR == null) {
            throw new RuntimeException("Failed to initialize blocks");
        }
        System.out.println("Initialized " + (Blocks.MAX_BLOCK_STATE_ID + 1) + " blocks in " + (System.currentTimeMillis() - s) + "ms");

        ServerSettings serverSettings = new ServerSettings();

        Server server = new Server(serverSettings);

        System.out.println("Server start done");

        System.out.println("Binding");
        server.bind();
        System.out.println("Binding done");

    }
}
