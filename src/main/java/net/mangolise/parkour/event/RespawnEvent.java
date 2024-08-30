package net.mangolise.parkour.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class RespawnEvent implements PlayerEvent {
    private final Player player;
    private final int deathCount;

    public RespawnEvent(Player player, int deathCount) {
        this.player = player;
        this.deathCount = deathCount;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public int getDeathCount() {
        return deathCount;
    }
}
