package net.mangolise.parkour;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.parkour.element.CubeEntity;
import net.mangolise.parkour.element.Door;
import net.mangolise.parkour.event.CheckpointEvent;
import net.mangolise.parkour.event.FinishEvent;
import net.mangolise.parkour.handler.PlaceHandler;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.sound.SoundEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.mangolise.parkour.ParkourGame.*;

public class ParkourUtil {
    private static final ConcurrentHashMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public static PlayerData getData(Player player) {
        return getData(player.getUuid());
    }

    public static PlayerData getData(UUID uuid) {
        return Objects.requireNonNullElseGet(playerDataMap.get(uuid), PlayerData::new);
    }

    public static void respawnPlayer(Player player, boolean reset) {
        PlayerData playerData = playerDataMap.get(player.getUuid());
        player.teleport(player.getRespawnPoint());
        playerData.canUseJumppad = true;
        playerData.discardCollectedItems();
        PlaceHandler.removeBlocks(player);

        if (!reset) {
            player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 0.5f, 1.0f));
            playerData.deathCount += 1;
        }
    }

    public static void resetPlayer(Player player) {
        // replace the old PlayerData with new PlayerData
        PlayerData newData = new PlayerData();
        newData.startTime = System.currentTimeMillis();
        playerDataMap.put(player.getUuid(), newData);

        player.setRespawnPoint(MapData.checkpoints.getFirst().getFirst());
        player.setAllowFlying(false);
        player.setFlying(false);
        respawnPlayer(player, true);

        despawnCubes(player);
        spawnCubes(player);

        for (Door door : MapData.doors) {
            door.deactivate(player);
        }

        player.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_RESONATE, Sound.Source.PLAYER, 1.0f, 1.5f));
    }

    public static void setCheckpoint(Player player, PlayerData playerData, Pos checkpoint, int index) {
        playerData.currentCheckpoint = index;
        playerData.applyCollectedItems();

        PlaceHandler.removeBlocks(player);
        player.setRespawnPoint(checkpoint.add(0d, 0.05d, 0d));

        // if normal checkpoint
        int checkpointCount = MapData.checkpoints.size();
        if (index != checkpointCount-1) {
            player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.BLOCK, 0.5f, 1.0f));
            GameSdkUtils.showTitle(player, 100, 1200, 100,
                    Component.empty(),
                    Component.text("Checkpoint reached " + index + "/" +
                            (checkpointCount - 1), NamedTextColor.DARK_GREEN));
            EventDispatcher.call(new CheckpointEvent(player, index));
        } else { // if win
            long timeSpent = System.currentTimeMillis() - playerData.startTime;
            int deathCount = playerData.deathCount;
            playerData.finishTime = timeSpent;

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

    public static void spawnCubes(Player player) {
        if (!MapData.cubeSpawns.isEmpty()) {
            List<CubeEntity> cubeList = new ArrayList<>();
            for (Vec pos : MapData.cubeSpawns) {
                cubeList.add(new CubeEntity(game.instance, player, pos));
            }

            game.cubes.put(player.getUuid(), cubeList);
        }
    }

    public static void despawnCubes(Player player) {
        if (!MapData.cubeSpawns.isEmpty()) {
            UUID uuid = player.getUuid();
            for (CubeEntity cube : Objects.requireNonNullElseGet(game.cubes.get(uuid), List::<CubeEntity>of)) {
                cube.remove();
            }

            game.cubes.remove(uuid);
        }
    }
}
