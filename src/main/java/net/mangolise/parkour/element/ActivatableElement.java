package net.mangolise.parkour.element;

import net.minestom.server.entity.Player;

public interface ActivatableElement {
    void activate(Player player);
    void deactivate(Player player);
}
