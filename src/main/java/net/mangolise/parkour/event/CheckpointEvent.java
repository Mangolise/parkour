package net.mangolise.parkour.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class CheckpointEvent implements PlayerEvent {
    private final Player player;
    private final int checkpointIndex;

    public CheckpointEvent(Player player, int checkpointIndex) {
        this.player = player;
        this.checkpointIndex = checkpointIndex;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public int getCheckpointIndex() {
        return checkpointIndex;
    }
}
