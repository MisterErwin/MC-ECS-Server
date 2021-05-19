package es.luepg.ecs.world.entity;

import com.artemis.Component;
import es.luepg.mcdata.data.game.world.Material;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Arrays;

/**
 * The entity has a player inventory
 * <p>
 * https://wiki.vg/Inventory
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
//ToDo: Separate main inventory & armor
public class PlayerInventoryComponent extends Component {
    /*
        helmet 5
        chest 6
        leggings 7
        boots 8
        main-inv (top left) 9
        main inv (bottom right) 35
        hotbar 36-44
        offhand 45

        0 crafting output
        1-4 crafting input
     */

    public PlayerInventoryComponent() {
        Arrays.fill(inventory, Material.AIR);
    }

    @Setter
    @Getter
    @NonNull
    private Material[] inventory = new Material[46];

    @Getter
    @Setter
    private int selectedHotBarIndex; //0-8

    public Material getSelectedItem() {
        return this.inventory[36 + this.selectedHotBarIndex];
    }

    public Material getOffhandItem() {
        return this.inventory[45];
    }

}
