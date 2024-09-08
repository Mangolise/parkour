package net.mangolise.parkour;

import net.kyori.adventure.sound.Sound;
import net.mangolise.parkour.element.Door;
import net.mangolise.parkour.event.RespawnEvent;
import net.mangolise.parkour.handler.PlaceHandler;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParkourPlayer extends Player {
    public int currentCheckpoint;
    public int deathCount;
    public boolean canUseJumppad;
    public boolean canSeeOthers;
    public long startTime; // the unix time in milliseconds when the player collected the last checkpoint
    public long finishTime; // time spent total after game has finished
    public long checkpointTime; // time spent to get to the last checkpoint (total from start)
    public @Nullable UUID currentlyHolding;
    public boolean hasMoved;

    private final Set<Integer> collectedItems;
    private final Set<Integer> newCollectedItems;

    public ParkourPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection connection) {
        super(uuid, username, connection);
        collectedItems = new HashSet<>();
        newCollectedItems = new HashSet<>();
        resetPlayerData();
    }

    private void resetPlayerData() {
        currentCheckpoint = 0;
        deathCount = 0;
        canUseJumppad = true;
        canSeeOthers = true;
        startTime = 0;
        finishTime = 0;
        checkpointTime = 0;
        currentlyHolding = null;
        hasMoved = false;
        collectedItems.clear();
        newCollectedItems.clear();
    }

    public void respawnToCheckpoint() {
        respawnToCheckpointImpl(false);
    }

    private void respawnToCheckpointImpl(boolean reset) {
        // if the player is on its first checkpoint and this isn't part of resetting, then just reset instead
        if (currentCheckpoint == 0 && !reset) {
            respawnToStart();
            return;
        }

        teleport(getRespawnPoint());
        canUseJumppad = true;
        discardCollectedItems();
        checkpointTime = calculateTimeSpent(System.currentTimeMillis());
        hasMoved = false;
        startTime = 0;
        PlaceHandler.removeBlocks(this);

        if (!reset) {
            playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 0.5f, 1.0f));
            deathCount += 1;
        }

        EventDispatcher.call(new RespawnEvent(this, deathCount));
    }

    public void respawnToStart() {
        resetPlayerData();

        setRespawnPoint(MapData.checkpoints.getFirst().getFirst());
        setAllowFlying(false);
        setFlying(false);
        respawnToCheckpointImpl(true);

        ParkourUtil.despawnCubes(this);
        ParkourUtil.spawnCubes(this);

        for (Door door : MapData.doors) {
            door.deactivate(this);
        }

        playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_RESONATE, Sound.Source.PLAYER, 1.0f, 1.5f));
    }

    public long calculateTimeSpent(long currentTime) {
        if (finishTime != 0) {
            return finishTime;
        } else if (startTime == 0) {
            return checkpointTime;
        }

        return checkpointTime + (currentTime - startTime);
    }

    public boolean hasCollectedItem(int item) {
        return newCollectedItems.contains(item) || collectedItems.contains(item);
    }

    public void addCollectedItem(int item) {
        newCollectedItems.add(item);
    }

    public void applyCollectedItems() {
        collectedItems.addAll(newCollectedItems);
        newCollectedItems.clear();
    }

    public void discardCollectedItems() {
        newCollectedItems.clear();
    }
}
