package net.mangolise.parkour;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
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
        player.setRespawnPoint(mapData.getCheckpoints().getFirst().getFirst());
        respawnPlayer(player, true);

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_RESONATE, Sound.Source.PLAYER, 1.0f, 1.5f));
    }

    public static String formatTime(long time) {
        return String.format("%02d", time / 60000) + ":" +
                String.format("%02d", time / 1000 % 60) + "." +
                String.format("%02d", time / 50 * 5 % 100);
    }
}
