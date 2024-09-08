package net.mangolise.parkour;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.parkour.element.CubeEntity;
import net.mangolise.parkour.event.CheckpointEvent;
import net.mangolise.parkour.event.FinishEvent;
import net.mangolise.parkour.handler.PlaceHandler;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.sound.SoundEvent;

import java.util.*;

import static net.mangolise.parkour.ParkourGame.*;

public class ParkourUtil {
    public static void setCheckpoint(ParkourPlayer player, Pos checkpoint, int index) {
        player.currentCheckpoint = index;
        player.applyCollectedItems();

        PlaceHandler.removeBlocks(player);
        player.setRespawnPoint(checkpoint);

        // if normal checkpoint
        int checkpointCount = MapData.checkpoints.size();
        if (index != checkpointCount-1) {
            long currentTime = System.currentTimeMillis();
            player.checkpointTime = player.calculateTimeSpent(currentTime);
            player.startTime = currentTime;

            player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.BLOCK, 0.5f, 1.0f));
            GameSdkUtils.showTitle(player, 100, 1200, 100,
                    Component.empty(),
                    Component.text("Checkpoint reached " + index + "/" +
                            (checkpointCount - 1), NamedTextColor.DARK_GREEN));
            EventDispatcher.call(new CheckpointEvent(player, index));
        } else { // if win
            long timeSpent = player.calculateTimeSpent(System.currentTimeMillis());
            int deathCount = player.deathCount;
            player.finishTime = timeSpent;

            String timeSpentDis = ChatUtil.formatTime(timeSpent);
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

    public static void spawnCubes(ParkourPlayer player) {
        if (!MapData.cubeSpawns.isEmpty()) {
            List<CubeEntity> cubeList = new ArrayList<>();
            for (Vec pos : MapData.cubeSpawns) {
                cubeList.add(new CubeEntity(game.instance, player, pos));
            }

            game.cubes.put(player.getUuid(), cubeList);
        }
    }

    public static void despawnCubes(ParkourPlayer player) {
        if (!MapData.cubeSpawns.isEmpty()) {
            UUID uuid = player.getUuid();
            for (CubeEntity cube : Objects.requireNonNullElseGet(game.cubes.get(uuid), List::<CubeEntity>of)) {
                cube.remove();
            }

            game.cubes.remove(uuid);
        }
    }
}
