package net.mangolise.parkour.handler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.parkour.MapData;
import net.mangolise.parkour.ParkourGame;
import net.mangolise.parkour.ParkourUtil;
import net.mangolise.parkour.PlayerData;
import net.mangolise.parkour.element.ItemPickup;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;

public class MovementHandler {
    public static void handlePlayerMoveEvent(PlayerMoveEvent e, ParkourGame game) {
        Pos newPos = e.getNewPosition();
        Player player = e.getPlayer();
        PlayerData playerData = ParkourUtil.getData(player);

        // Checkpoints
        List<List<Pos>> checkpointss = MapData.checkpoints;
        int currentCheckpoint = playerData.currentCheckpoint;
        int checkpointCount = checkpointss.size();

        for (int i = currentCheckpoint+1; i < checkpointCount; i++) {
            List<Pos> checkpoints = checkpointss.get(i);

            for (Pos checkpoint : checkpoints) {
                if (isAtPosOr1Above(checkpoint, newPos)) {
                    if (i != currentCheckpoint + 1) { // this is not the next checkpoint, they skipped a checkpoint
                        player.sendActionBar(Component.text("You skipped a checkpoint!", NamedTextColor.DARK_RED));
                        continue;
                    }

                    ParkourUtil.setCheckpoint(player, playerData, checkpoint, i);
                }
            }
        }

        // Item pickups
        for (int i = 0; i < MapData.itemPickups.size(); i++) {
            ItemPickup pickup = MapData.itemPickups.get(i);
            for (Vec position : pickup.positions()) {
                if (!playerData.hasCollectedItem(i) && isAtPosOr1Above(position, newPos)) {
                    player.getInventory().addItemStack(pickup.item());
                    playerData.addCollectedItem(i);
                    break;
                }
            }
        }

        Block currentBlock = game.instance.getBlock(e.getNewPosition().add(0d, 0.5d, 0d));
        Block belowBlock = game.instance.getBlock(e.getNewPosition().sub(0d, 0.5d, 0d));

        // Jump pad blocks
        if (belowBlock.compare(Block.EMERALD_BLOCK)) {
            if (playerData.canUseJumppad) {
                player.setVelocity(new Vec(0d, 24d, 0d));
                playerData.canUseJumppad = false;

                MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                    playerData.canUseJumppad = true;
                    return TaskSchedule.stop();
                }, TaskSchedule.tick(5));
            }
        }

        // Death
        if (player.getPosition().y() < MapData.deathLevel ||
                currentBlock.compare(Block.LAVA) || currentBlock.compare(Block.WATER)) {
            ParkourUtil.respawnPlayer(player, false);
        }
    }

    private static boolean isAtPosOr1Above(Point point, Point newPos) {
        return point.blockX() == newPos.blockX() && point.blockZ() == newPos.blockZ() &&
                (point.blockY() == newPos.blockY() || point.blockY() + 1 == newPos.blockY());
    }
}
