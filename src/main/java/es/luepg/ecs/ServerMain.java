package es.luepg.ecs;

import com.github.steveice10.mc.protocol.data.game.world.block.Blocks;
import es.luepg.ecs.server.Server;
import es.luepg.ecs.server.ServerSettings;

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
        System.out.println("Initialized blocks in " + (System.currentTimeMillis() - s) + "ms");

        ServerSettings serverSettings = new ServerSettings();

        Server server = new Server(serverSettings);

        System.out.println("Server start done");

        System.out.println("Binding");
        server.bind();
        System.out.println("Binding done");

    }
}
