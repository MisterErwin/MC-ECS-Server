package es.luepg.ecs.server;

import lombok.Getter;

import java.net.Proxy;

/**
 * @author elmexl
 * Created on 31.05.2019.
 */
@Getter
public class ServerSettings {
    private final boolean VERIFY_USERS = false;
    private final String HOST = "0.0.0.0";
    private final int PORT = 25565;
    private final Proxy PROXY = Proxy.NO_PROXY;
    private final Proxy AUTH_PROXY = Proxy.NO_PROXY;


}
