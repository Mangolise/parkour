package net.mangolise.parkour.element;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Door {
    private final Instance instance;
    private final Point pos;
    private final Block block;
    private final Set<UUID> openPlayers = new HashSet<>();

    public Door(Instance instance, Point pos) {
        this.instance = instance;
        this.pos = pos;
        this.block = Block.COAL_BLOCK;

        instance.setBlock(pos, block);
        instance.setBlock(pos.add(0, 1, 0), block);
    }

    public void open(Player player) {
        setBlocksPacket(player, Block.AIR);
        openPlayers.add(player.getUuid());
    }

    public void close(Player player) {
        setBlocksPacket(player, Block.COAL_BLOCK);
        openPlayers.remove(player.getUuid());
    }

    public void setBlocks(Player player, boolean end) {
        setBlocksReal(openPlayers.contains(player.getUuid()) == end, end);
    }

    private void setBlocksPacket(Player player, Block block) {
        player.sendPacket(new BlockChangePacket(pos, block));
        player.sendPacket(new BlockChangePacket(pos.add(0, 1, 0), block));
    }

    private void setBlocksReal(boolean exist, boolean doPacket) {
        Block newBlock = exist ? block : Block.AIR;
        if (instance.getBlock(pos).compare(newBlock)) {
            return;
        }

        instance.setBlock(pos, newBlock);
        instance.setBlock(pos.add(0, 1, 0), newBlock);

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
