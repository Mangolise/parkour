package net.mangolise.parkour.handler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.parkour.MapData;
import net.mangolise.parkour.ParkourGame;
import net.mangolise.parkour.ParkourUtil;
import net.mangolise.parkour.ParkourPlayer;
import net.mangolise.parkour.element.ItemPickup;
import net.mangolise.parkour.event.JumpPadEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MovementHandler {
    public static void handlePlayerMoveEvent(PlayerMoveEvent e, ParkourGame game) {
        Pos newPos = e.getNewPosition();
        ParkourPlayer player = (ParkourPlayer) e.getPlayer();

        // if the player didn't move (they turned their head)
        if (newPos.distanceSquared(player.getPosition()) < Vec.EPSILON) {
            return;
        }

        if (!player.hasMoved) {
            player.hasMoved = true;
            player.startTime = System.currentTimeMillis();
        }

        // Checkpoints
        List<List<Pos>> checkpointss = MapData.checkpoints;
        int currentCheckpoint = player.currentCheckpoint;
        int checkpointCount = checkpointss.size();

        for (int i = currentCheckpoint+1; i < checkpointCount; i++) {
            List<Pos> checkpoints = checkpointss.get(i);

            for (Pos checkpoint : checkpoints) {
                if (isAtPosOr1Above(player, checkpoint, newPos)) {
                    if (i != currentCheckpoint + 1) { // this is not the next checkpoint, they skipped a checkpoint
                        player.sendActionBar(Component.text("You skipped a checkpoint!", NamedTextColor.DARK_RED));
                        continue;
                    }

                    ParkourUtil.setCheckpoint(player, checkpoint, i);
                }
            }
        }

        // Item pickups
        for (int i = 0; i < MapData.itemPickups.size(); i++) {
            ItemPickup pickup = MapData.itemPickups.get(i);
            for (Vec pickupPos : pickup.positions()) {
                if (!player.hasCollectedItem(i) && isAtPosOr1Above(player, pickupPos, newPos)) {
                    player.getInventory().addItemStack(pickup.item());
                    player.addCollectedItem(i);
                    break;
                }
            }
        }

        Set<Integer> currentBlock = getBoundingBoxFeetBlockIds(game.instance, player.getBoundingBox(), e.getNewPosition().add(0d, 0.125d, 0d));
        Set<Integer> belowBlock = getBoundingBoxFeetBlockIds(game.instance, player.getBoundingBox(), e.getNewPosition().sub(0d, 0.5d, 0d));

        // Jump pad blocks
        if (belowBlock.contains(Block.EMERALD_BLOCK.id())) {
            if (player.canUseJumppad) {
                player.setVelocity(new Vec(0d, 24d, 0d));
                player.canUseJumppad = false;

                MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                    player.canUseJumppad = true;
                    return TaskSchedule.stop();
                }, TaskSchedule.tick(5));

                EventDispatcher.call(new JumpPadEvent(player));
            }
        }

        // Death
        if (player.getPosition().y() < MapData.deathLevel ||
                currentBlock.contains(Block.LAVA.id()) || currentBlock.contains(Block.WATER.id())) {
            player.respawnToCheckpoint();
        }
    }

    private static boolean isAtPosOr1Above(Player player, Point point, Point newPos) {
        return GameSdkUtils.collidesWithBoundingBox(player.getBoundingBox(), newPos, new BlockVec(point)) ||
                GameSdkUtils.collidesWithBoundingBox(player.getBoundingBox(), newPos, new BlockVec(point.add(0, 1, 0)));
    }

    private static Set<Integer> getBoundingBoxFeetBlockIds(Instance instance, BoundingBox box, Point offset) {
        Set<BlockVec> points = new HashSet<>();

        // get the rounded points for all four corners and add them to a set to remove duplicates
        // center isn't needed because the player is less a block wide
        points.add(new BlockVec(offset.add(box.minX(), box.minY(), box.minZ())));
        points.add(new BlockVec(offset.add(box.minX(), box.minY(), box.maxZ())));
        points.add(new BlockVec(offset.add(box.maxX(), box.minY(), box.minZ())));
        points.add(new BlockVec(offset.add(box.maxX(), box.minY(), box.maxZ())));

        // get the blocks on the corners found and return them
        Set<Integer> blocks = new HashSet<>();
        for (Point point : points) {
            blocks.add(instance.getBlock(point).id());
        }

        return blocks;
    }
}
