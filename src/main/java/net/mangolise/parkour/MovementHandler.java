package net.mangolise.parkour;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.gamesdk.util.Util;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;

import static net.mangolise.parkour.ParkourGame.*;

public class MovementHandler {
    public static void handlePlayerMoveEvent(PlayerMoveEvent e, ParkourGame game) {
        Pos newPos = e.getNewPosition();
        Player player = e.getPlayer();

        List<List<Pos>> checkpointss = game.mapData.getCheckpoints();
        int currentCheckpoint = player.getTag(CURRENT_CHECKPOINT_TAG);
        int checkpointCount = checkpointss.size();

        for (int i = currentCheckpoint+1; i < checkpointCount; i++) {
            List<Pos> checkpoints = checkpointss.get(i);

            for (Pos checkpoint : checkpoints) {
                if (checkpoint.blockX() == newPos.blockX() && checkpoint.blockZ() == newPos.blockZ() &&
                        (checkpoint.blockY() == newPos.blockY() || checkpoint.blockY() + 1 == newPos.blockY())) {

                    if (i != currentCheckpoint + 1) { // this is not the next checkpoint, they skipped a checkpoint
                        player.sendActionBar(Component.text("You skipped a checkpoint!", NamedTextColor.DARK_RED));
                        continue;
                    }

                    player.setRespawnPoint(checkpoint);
                    player.setTag(CURRENT_CHECKPOINT_TAG, i);

                    // if normal checkpoint
                    if (i != checkpointCount-1) {
                        player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.BLOCK, 0.5f, 1.0f));
                        Util.showTitle(player, 100, 1200, 100,
                                Component.empty(),
                                Component.text("Checkpoint reached " + i + "/" +
                                        (checkpointCount - 1), NamedTextColor.DARK_GREEN));
                    }
                    else { // if win
                        long timeSpent = System.currentTimeMillis() - player.getTag(START_TIME_TAG);
                        int deathCount = player.getTag(DEATH_COUNT_TAG);
                        player.setTag(FINISH_TIME_TAG, timeSpent);

                        String subtitle = ParkourUtil.formatTime(timeSpent) + ", " + deathCount + " death";
                        if (deathCount != 1) subtitle += 's';

                        player.setAllowFlying(true);

                        final int stayTime = 6000;
                        if (false) { // if new personal best
                            player.playSound(Sound.sound(SoundEvent.UI_TOAST_CHALLENGE_COMPLETE, Sound.Source.BLOCK, 0.5f, 1.0f));

                            Util.showTitle(player, 100, stayTime, 100,
                                    Component.text("New Personal Best!"),
                                    Component.text(subtitle, NamedTextColor.GREEN));
                        } else { // normal win
                            player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.BLOCK, 0.5f, 1.0f));

                            Util.showTitle(player, 100, stayTime, 100,
                                    Component.text("You Won!"),
                                    Component.text(subtitle, NamedTextColor.GREEN));
                        }
                    }
                }
            }
        }

        Block currentBlock = game.instance.getBlock(e.getNewPosition().add(0d, 0.5d, 0d));
        Block belowBlock = game.instance.getBlock(e.getNewPosition().sub(0d, 0.5d, 0d));

        // Jump pad blocks
        if (belowBlock.compare(Block.EMERALD_BLOCK)) {
            if (player.getTag(CAN_JUMPPAD_TAG)) {
                player.setVelocity(new Vec(0d, 24d, 0d));
                player.setTag(CAN_JUMPPAD_TAG, false);

                MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                    player.setTag(CAN_JUMPPAD_TAG, true);
                    return TaskSchedule.stop();
                }, TaskSchedule.tick(5));
            }
        }

        // Death blocks
        if (currentBlock.compare(Block.LAVA) || currentBlock.compare(Block.WATER)) {
            ParkourUtil.respawnPlayer(player, false);
        }
    }
}
