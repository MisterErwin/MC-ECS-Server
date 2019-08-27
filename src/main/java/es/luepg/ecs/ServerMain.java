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

        if (Blocks.AIR == null) {
            throw new RuntimeException("Failed to start");
        }

        ServerSettings serverSettings = new ServerSettings();

        Server server = new Server(serverSettings);

        System.out.println("--- Started ---");

        System.out.println("Binding");
        System.out.println(Thread.currentThread().getId() + ": " + Thread.currentThread().getName());
        server.bind();
        System.out.println("Bind end");

    }
}
