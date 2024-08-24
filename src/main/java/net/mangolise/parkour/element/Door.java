package net.mangolise.parkour.element;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Door implements ActivatableElement {
    private final Instance instance;
    private final List<Vec> positions;
    private final Block block;
    private final Set<UUID> openPlayers = new HashSet<>();

    public Door(Instance instance, List<Vec> pos) {
        this.instance = instance;
        this.positions = pos;
        this.block = Block.POLISHED_TUFF;

        for (Vec position : positions) {
            instance.setBlock(position, block);
            instance.setBlock(position.add(0, 1, 0), block);
            instance.setBlock(position.add(0, 2, 0), block);
        }
    }

    @Override
    public void activate(Player player) {
        setBlocksPacket(player, Block.AIR);
        openPlayers.add(player.getUuid());
    }

    @Override
    public void deactivate(Player player) {
        setBlocksPacket(player, Block.COAL_BLOCK);
        openPlayers.remove(player.getUuid());
    }

    public void setBlocks(Player player, boolean end) {
        setBlocksReal(openPlayers.contains(player.getUuid()) == end, end);
    }

    private void setBlocksPacket(Player player, Block block) {
        for (Vec position : positions) {
            player.sendPacket(new BlockChangePacket(position, block));
            player.sendPacket(new BlockChangePacket(position.add(0, 1, 0), block));
            player.sendPacket(new BlockChangePacket(position.add(0, 2, 0), block));
        }
    }

    private void setBlocksReal(boolean exist, boolean doPacket) {
        Block newBlock = exist ? block : Block.AIR;
        if (instance.getBlock(positions.getFirst()).compare(newBlock)) {
            return;
        }

        for (Vec position : positions) {
            instance.setBlock(position, newBlock);
            instance.setBlock(position.add(0, 1, 0), newBlock);
            instance.setBlock(position.add(0, 2, 0), newBlock);
        }

        if (!doPacket) {
            return;
        }

        if (exist) {
            for (UUID uuid : openPlayers) {
                Player p = instance.getPlayerByUuid(uuid);
                assert p != null;
                setBlocksPacket(p, Block.AIR);
            }
        } else {
            for (Player p : instance.getPlayers()) {
                if (openPlayers.contains(p.getUuid())) {
                    continue;
                }

                setBlocksPacket(p, block);
            }
        }
    }
}
