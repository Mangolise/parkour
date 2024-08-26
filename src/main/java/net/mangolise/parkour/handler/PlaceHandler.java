package net.mangolise.parkour.handler;

import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.parkour.MapData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerDiggingPacket;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.world.DimensionType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceHandler {
    public static final Map<UUID, List<WorldBlock>> playerBlocks = new ConcurrentHashMap<>();

    public static void setup(Instance instance) {
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();

        // intercept placing before minestom does anything
        events.addListener(PlayerPacketEvent.class, e -> {
            if (e.getPacket() instanceof ClientPlayerBlockPlacementPacket packet) {
                Player player = e.getPlayer();
                Player.Hand hand = packet.hand();
                ItemStack clickedItem = player.getItemInHand(hand);
                Material material = clickedItem.material();

                // if they just clicked the floor with an item that cant be placed
                if (!clickedItem.material().isBlock()) {
                    return;
                }

                e.setCancelled(true);

                // if it's a menu item then do that stuff
                if (ItemHandler.handlePlayerUseItemEvent(player, hand, material)) {
                    return;
                }

                // packet.blockPosition() returns the block that was clicked, not the new blocks position
                Block newBlock = material.block();
                Point pos = packet.blockPosition().add(packet.blockFace().toDirection().vec());

                {
                    Block oldBlock = instance.getBlock(pos);
                    DimensionType instanceDim = instance.getCachedDimensionType();

                    final Set<Integer> replaceableBlocks = Set.of(Block.AIR.id(), Block.LAVA.id(), Block.WATER.id());

                    // check if they can place the block
                    if (player.getGameMode() != GameMode.SURVIVAL || !replaceableBlocks.contains(oldBlock.id()) ||
                            pos.y() >= instanceDim.maxY() || pos.y() < instanceDim.minY() || GameSdkUtils.collidesWithBoundingBox(player.getBoundingBox(), player.getPosition(), pos)) {
                        player.sendPackets(new BlockChangePacket(pos, oldBlock), new AcknowledgeBlockChangePacket(packet.sequence()));
                        return;
                    }
                }


                // tell the client that they did place the block and add it to the list of blocks that they placed
                playerBlocks.get(player.getUuid()).add(new WorldBlock(new BlockVec(pos), newBlock));
                player.sendPackets(new BlockChangePacket(pos, newBlock), new AcknowledgeBlockChangePacket(packet.sequence()));

                // get rid of one of the blocks
                player.getInventory().setItemInHand(hand, clickedItem.consume(1));
            } else if (e.getPacket() instanceof ClientPlayerDiggingPacket packet && packet.status() == ClientPlayerDiggingPacket.Status.STARTED_DIGGING) {
                // if they start digging all blocks disappear
                e.setCancelled(true);
            }
        });
    }

    public static void removeBlocks(Player player) {
        List<WorldBlock> blocks = playerBlocks.get(player.getUuid());
        List<SendablePacket> packets = new ArrayList<>(blocks.size());
        for (WorldBlock block : blocks) {
            packets.add(new BlockChangePacket(block.pos(), Block.AIR));
        }

        blocks.clear();
        player.sendPackets(packets);

        // remove all picked up materials
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (MapData.pickupMaterials.contains(inv.getItemStack(i).material())) {
                inv.setItemStack(i, ItemStack.AIR);
            }
        }
    }

    public record WorldBlock(BlockVec pos, Block block) { }
}
