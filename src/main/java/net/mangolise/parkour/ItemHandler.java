package net.mangolise.parkour;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import static net.mangolise.parkour.ParkourGame.*;

public class ItemHandler {
    public static void giveGameItems(Player player) {
        player.getInventory().addItemStack(ItemStack.builder(Material.STICK)
                .customName(Component.text("Respawn")).build());
        player.getInventory().addItemStack(ItemStack.builder(Material.DEAD_BUSH)
                .customName(Component.text("Restart")).build());
        player.getInventory().addItemStack(ItemStack.builder(Material.ENDER_EYE)
                .customName(Component.text("Hide other players")).build());
        player.getInventory().addItemStack(ItemStack.builder(Material.OXIDIZED_COPPER_DOOR)
                .customName(Component.text("Leave")).build());
    }

    public static void handlePlayerUseItemEvent(PlayerUseItemEvent e, ParkourGame game) {
        e.setCancelled(true);
        e.setItemUseTime(0);

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
                other.updateViewerRule();
            }

            player.getInventory().setItemInHand(e.getHand(), ItemStack.builder(Material.ENDER_PEARL)
                    .customName(Component.text("Show other players")).build());
        }
        else if (mat == Material.ENDER_PEARL) {
            player.setTag(CAN_SEE_OTHERS_TAG, true);
            for (Player other : game.instance.getPlayers()) {
                other.updateViewerRule();
            }

            player.getInventory().setItemInHand(e.getHand(), ItemStack.builder(Material.ENDER_EYE)
                    .customName(Component.text("Hide other players")).build());
        }
        else if (mat == Material.OXIDIZED_COPPER_DOOR) {
            // TODO: leaving
        }
    }
}
