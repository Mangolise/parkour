package net.mangolise.parkour;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.gamesdk.util.Util;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.sound.SoundEvent;

import static net.mangolise.parkour.ParkourGame.*;

public class ParkourUtil {
    public static void respawnPlayer(Player player, boolean reset) {
        player.teleport(player.getRespawnPoint());
        player.removeTag(CAN_JUMPPAD_TAG);

        if (!reset) {
            player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 0.5f, 1.0f));
            player.setTag(DEATH_COUNT_TAG, player.getTag(DEATH_COUNT_TAG) + 1);
        }
    }

    public static void resetPlayer(Player player, MapData mapData) {
        player.setTag(START_TIME_TAG, System.currentTimeMillis());
        player.removeTag(CURRENT_CHECKPOINT_TAG);
        player.removeTag(CAN_JUMPPAD_TAG);
        player.removeTag(FINISH_TIME_TAG);
        player.removeTag(DEATH_COUNT_TAG);
        player.setRespawnPoint(mapData.checkpoints.getFirst().getFirst());
        player.setAllowFlying(false);
        player.setFlying(false);
        respawnPlayer(player, true);

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_RESONATE, Sound.Source.PLAYER, 1.0f, 1.5f));
    }

    public static void setCheckpoint(Player player, Pos checkpoint, int index) {
        player.setRespawnPoint(checkpoint);
        player.setTag(CURRENT_CHECKPOINT_TAG, index);

        // if normal checkpoint
        int checkpointCount = game.mapData.checkpoints.size();
        if (index != checkpointCount-1) {
            player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.BLOCK, 0.5f, 1.0f));
            GameSdkUtils.showTitle(player, 100, 1200, 100,
                    Component.empty(),
                    Component.text("Checkpoint reached " + index + "/" +
                            (checkpointCount - 1), NamedTextColor.DARK_GREEN));
        }
        else { // if win
            long timeSpent = System.currentTimeMillis() - player.getTag(START_TIME_TAG);
            int deathCount = player.getTag(DEATH_COUNT_TAG);
            player.setTag(FINISH_TIME_TAG, timeSpent);

            String timeSpentDis =  ParkourUtil.formatTime(timeSpent);
            String subtitle = timeSpentDis + ", " + deathCount + " death";
            if (deathCount != 1) subtitle += 's';

            player.setAllowFlying(true);
            player.sendMessage(Component.text(
                    String.format("You won! Time: %s, Deaths: %d!", timeSpentDis, deathCount), NamedTextColor.GREEN));

            final int stayTime = 6000;
            FinishEvent event = new FinishEvent(player, timeSpent, deathCount);
            EventDispatcher.call(event);
            if (event.isNewRecord()) { // if new personal best
                player.playSound(Sound.sound(SoundEvent.UI_TOAST_CHALLENGE_COMPLETE, Sound.Source.BLOCK, 0.5f, 1.0f));

                GameSdkUtils.showTitle(player, 100, stayTime, 100,
                        Component.text("New Personal Best!"),
                        Component.text(subtitle, NamedTextColor.GREEN));
            } else { // normal win
                player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.BLOCK, 0.5f, 1.0f));

                GameSdkUtils.showTitle(player, 100, stayTime, 100,
                        Component.text("You Won!"),
                        Component.text(subtitle, NamedTextColor.GREEN));
            }
        }
    }

    public static String formatTime(long time) {
        return String.format("%02d", time / 60000) + ":" +
                String.format("%02d", time / 1000 % 60) + "." +
                String.format("%02d", time / 50 * 5 % 100);
    }
}
