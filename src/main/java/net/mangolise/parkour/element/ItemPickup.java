package net.mangolise.parkour.element;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;

import java.util.List;

public record ItemPickup(List<Vec> positions, ItemStack item) { }
