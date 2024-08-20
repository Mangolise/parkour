package net.mangolise.parkour;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import static net.mangolise.parkour.ParkourGame.*;

public class ItemHandler {
    public static void giveGameItems(Player player) {
        player.getInventory().addItemStack(createMenuItem(Material.STICK, "Respawn"));
        player.getInventory().addItemStack(createMenuItem(Material.DEAD_BUSH, "Restart"));
        player.getInventory().addItemStack(createMenuItem(Material.ENDER_EYE, "Hide other players"));
        player.getInventory().addItemStack(createMenuItem(Material.OXIDIZED_COPPER_DOOR, "Leave"));
    }

    private static ItemStack createMenuItem(Material mat, String name) {
        return ItemStack.builder(mat)
                .customName(Component.text(name).decoration(TextDecoration.ITALIC, false))
                .build();
    }

    public static void handlePlayerUseItemEvent(PlayerUseItemEvent e, ParkourGame game) {
        e.setCancelled(true);

        Player player = e.getPlayer();
        Material mat = e.getItemStack().material();

        if (mat == Material.STICK) {
            ParkourUtil.respawnPlayer(player, false);
        }
        else if (mat == Material.DEAD_BUSH) {
            ParkourUtil.resetPlayer(player, game.mapData);
        }
        else if (mat == Material.ENDER_EYE) {
            player.setTag(CAN_SEE_OTHERS_TAG, false);
            for (Player other : game.instance.getPlayers()) {
                other.updateViewableRule();
            }

            player.getInventory().setItemInHand(e.getHand(), createMenuItem(Material.ENDER_PEARL, "Show other players"));
        }
        else if (mat == Material.ENDER_PEARL) {
            player.setTag(CAN_SEE_OTHERS_TAG, true);
            for (Player other : game.instance.getPlayers()) {
                other.updateViewableRule();
            }

            player.getInventory().setItemInHand(e.getHand(), createMenuItem(Material.ENDER_EYE, "Hide other players"));
        }
        else if (mat == Material.OXIDIZED_COPPER_DOOR) {
            player.kick("Bye!");
        }
    }
}
