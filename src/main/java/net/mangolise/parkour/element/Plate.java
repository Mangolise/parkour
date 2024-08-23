package net.mangolise.parkour.element;

import net.kyori.adventure.sound.Sound;
import net.mangolise.parkour.MapData;
import net.mangolise.parkour.ParkourGame;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Plate implements BlockHandler {
    private final String targetType;
    private final int target;

    // is a set bc it can be pressed on unpressed on each player separately
    private final Set<UUID> pressedPlayers = new HashSet<>();

    public Plate(Instance instance, Point pos, String targetType, int target) {
        this.targetType = targetType;
        this.target = target;

        instance.setBlock(pos, createPlate("0").withHandler(this));
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NamespaceID.from("parkour:plate");
    }

    @Override
    public boolean isTickable() {
        return true;
    }

    @Override
    public void tick(@NotNull BlockHandler.Tick tick) {
        BlockHandler.super.tick(tick);
        Point pos = tick.getBlockPosition();

        for (Map.Entry<UUID, List<CubeEntity>> cubes : ParkourGame.game.cubes.entrySet()) {
            UUID uuid = cubes.getKey();
            Player player = ParkourGame.game.instance.getPlayerByUuid(uuid);
            assert player != null;

            boolean lastPressed = pressedPlayers.contains(uuid);
            boolean pressed = hasCube(player, cubes.getValue(), pos);
            boolean justPressed = pressed && !lastPressed;
            boolean justReleased = !pressed && lastPressed;

            if (!justPressed && !justReleased) {
                continue;
            }

            if (justPressed) {
                pressedPlayers.add(uuid);
                player.sendPacket(new BlockChangePacket(pos, createPlate("15")));
                player.playSound(Sound.sound(SoundEvent.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON, Sound.Source.BLOCK,
                        1f, 1f), pos);
            } else { // released
                pressedPlayers.remove(uuid);
                player.sendPacket(new BlockChangePacket(pos, createPlate("0")));
                player.playSound(Sound.sound(SoundEvent.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF, Sound.Source.BLOCK,
                        1f, 1f), pos);
            }

            switch (targetType) {
                case "door" -> {
                    if (justPressed) {
                        MapData.doors.get(target).open(player);
                    } else {
                        MapData.doors.get(target).close(player);
                    }
                }
            }
        }
    }

    private boolean hasCube(Player player, List<CubeEntity> cubes, Point pos) {
        if (player.getPosition().sameBlock(pos)) {
            return true;
        }

        for (CubeEntity cube : cubes) {
            if (cube.getPosition().add(0.5).sameBlock(pos)) {
                return true;
            }
        }

        return false;
    }

    private Block createPlate(String power) {
        return Block.HEAVY_WEIGHTED_PRESSURE_PLATE.withProperty("power", power);
    }
}
