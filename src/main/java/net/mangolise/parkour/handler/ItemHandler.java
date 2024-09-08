package net.mangolise.parkour.handler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.parkour.ParkourGame;
import net.mangolise.parkour.ParkourUtil;
import net.mangolise.parkour.ParkourPlayer;
import net.mangolise.parkour.event.LeaveEvent;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

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

    public static boolean handlePlayerUseItemEvent(ParkourPlayer player, Player.Hand hand, Material mat) {
        if (mat == Material.STICK) {
            ParkourUtil.respawnPlayer(player, false);
        }
        else if (mat == Material.DEAD_BUSH) {
            ParkourUtil.resetPlayer(player);
        }
        else if (mat == Material.ENDER_EYE) {
            player.canSeeOthers = false;
            for (Player other : ParkourGame.game.instance.getPlayers()) {
                other.updateViewableRule();
            }

            player.getInventory().setItemInHand(hand, createMenuItem(Material.ENDER_PEARL, "Show other players"));
        }
        else if (mat == Material.ENDER_PEARL) {
            player.canSeeOthers = true;
            for (Player other : ParkourGame.game.instance.getPlayers()) {
                other.updateViewableRule();
            }

            player.getInventory().setItemInHand(hand, createMenuItem(Material.ENDER_EYE, "Hide other players"));
        }
        else if (mat == Material.OXIDIZED_COPPER_DOOR) {
            EventDispatcher.call(new LeaveEvent(player));
        }
        else {
            return false;
        }

        return true;
    }
}
