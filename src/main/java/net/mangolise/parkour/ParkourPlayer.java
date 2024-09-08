package net.mangolise.parkour;

import net.minestom.server.entity.Player;
import net.minestom.server.network.player.PlayerConnection;
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
    public long startTime;
    public long finishTime;
    public @Nullable UUID currentlyHolding;

    private final Set<Integer> collectedItems;
    private final Set<Integer> newCollectedItems;

    public ParkourPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection connection) {
        super(uuid, username, connection);
        collectedItems = new HashSet<>();
        newCollectedItems = new HashSet<>();
        resetPlayerData();
    }

    public void resetPlayerData() {
        currentCheckpoint = 0;
        deathCount = 0;
        canUseJumppad = true;
        canSeeOthers = true;
        startTime = 0;
        finishTime = 0;
        currentlyHolding = null;
        collectedItems.clear();
        newCollectedItems.clear();
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
