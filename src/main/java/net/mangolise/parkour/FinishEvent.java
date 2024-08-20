package net.mangolise.parkour;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class FinishEvent implements PlayerEvent {

    private final Player player;
    private final long msTimeSpent;
    private final int deathCount;
    private boolean isNewRecord = false;

    public FinishEvent(Player player, long msTimeSpent, int deathCount) {
        this.player = player;
        this.msTimeSpent = msTimeSpent;
        this.deathCount = deathCount;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public boolean isNewRecord() {
        return isNewRecord;
    }

    public void setIsNewRecord(boolean isNewRecord) {
        this.isNewRecord = isNewRecord;
    }

    public long getMsTimeSpent() {
        return msTimeSpent;
    }

    public int getDeathCount() {
        return deathCount;
    }
}
