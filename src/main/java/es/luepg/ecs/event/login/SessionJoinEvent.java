package es.luepg.ecs.event.login;

import com.github.steveice10.packetlib.Session;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author elmexl
 * Created on 31.05.2019.
 */
@AllArgsConstructor
public class SessionJoinEvent {
    @Getter
    private final Session session;

}
