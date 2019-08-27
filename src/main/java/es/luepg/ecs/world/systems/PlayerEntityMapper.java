package es.luepg.ecs.world.systems;

import com.artemis.BaseEntitySystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.All;
import com.github.steveice10.packetlib.Session;
import es.luepg.ecs.world.entity.PlayerComponent;
import es.luepg.ecs.world.util.SessionUtils;
import lombok.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A mapper that maps players (their UUID) to their entity id
 *
 * @author elmexl
 * Created on 18.07.2019.
 */
@All(PlayerComponent.class)
public class PlayerEntityMapper extends BaseEntitySystem {
    private final Map<UUID, Integer> uuidToEntityID = new HashMap<>();

    private ComponentMapper<PlayerComponent> mPlayers;

    @Override
    protected boolean checkProcessing() {
        return false;
    }

    @Override
    protected void processSystem() {

    }


    @Override
    public void inserted(int eid) {
        uuidToEntityID.put(mPlayers.get(eid).getGameProfile().getId(), eid);
    }

    @Override
    public void removed(int eid) {
        if (!mPlayers.has(eid)) {
            System.err.println("UNABLE TO REMOVE " + eid + " from the PlayerEntityMapper");
            return;
        }
        this.uuidToEntityID.remove(mPlayers.get(eid).getGameProfile().getId());
    }

    public Integer getEntityIDForPlayer(@NonNull UUID uuid) {
        return this.uuidToEntityID.get(uuid);
    }

    public Entity getEntityForPlayer(@NonNull UUID uuid) {
        Integer id = getEntityIDForPlayer(uuid);
        if (id == null)
            return null;
        return world.getEntity(id);
    }

    public Entity getEntityForPlayer(@NonNull Session session) {
        return world.getEntity(getEntityIDForPlayer(SessionUtils.getGameProfile(session).getId()));
    }

    public Collection<Entity> listPlayers() {
        return this.uuidToEntityID.values().stream().map(world::getEntity).collect(Collectors.toSet());
    }
}
