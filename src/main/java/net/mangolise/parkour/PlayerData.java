package net.mangolise.parkour;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    public int currentCheckpoint;
    public int deathCount;
    public boolean canUseJumppad;
    public boolean canSeeOthers;
    public long startTime;
    public long finishTime;
    public @Nullable UUID currentlyHolding;

    private final Set<Integer> collectedItems;
    private final Set<Integer> newCollectedItems;

    public PlayerData() {
        currentCheckpoint = 0;
        deathCount = 0;
        canUseJumppad = true;
        canSeeOthers = true;
        startTime = 0;
        finishTime = 0;
        currentlyHolding = null;
        collectedItems = new HashSet<>();
        newCollectedItems = new HashSet<>();
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
