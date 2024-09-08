package net.mangolise.parkour;

import net.kyori.adventure.text.Component;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.*;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.parkour.command.CheckpointCommand;
import net.mangolise.parkour.element.CubeEntity;
import net.mangolise.parkour.handler.ItemHandler;
import net.mangolise.parkour.handler.MovementHandler;
import net.mangolise.parkour.handler.PlaceHandler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourGame extends BaseGame<ParkourGame.Config> {
    public static @UnknownNullability ParkourGame game;

    public final Map<UUID, List<CubeEntity>> cubes = new ConcurrentHashMap<>();
    public @UnknownNullability Instance instance;

    public ParkourGame(Config config) {
        super(config);
        game = this;
    }

    @Override
    public void setup() {
        super.setup();
        instance = MinecraftServer.getInstanceManager().createInstanceContainer(
                GameSdkUtils.getPolarLoaderFromResource("worlds/" + config.worldName + ".polar"));
        instance.enableAutoChunkLoad(true);

        // Load MapData
        try {
            MapData.loadMapData(instance, config.worldName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MinecraftServer.getConnectionManager().setPlayerProvider(ParkourPlayer::new);

        MinecraftServer.getCommandManager().register(new CheckpointCommand());

        // Player spawning
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setGameMode(MapData.itemPickups.isEmpty() ? GameMode.ADVENTURE : GameMode.SURVIVAL);
        });

        events.addListener(PlayerSpawnEvent.class, e -> {
            ParkourPlayer player = (ParkourPlayer) e.getPlayer();

            PlaceHandler.playerBlocks.put(player.getUuid(), new ArrayList<>());

            // init stuff
            player.setRespawnPoint(MapData.checkpoints.getFirst().getFirst());
            player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED).setBaseValue(-128);
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(-128);

            ItemHandler.giveGameItems(player);
            player.respawnToStart();
            player.updateViewableRule(viewer -> ((ParkourPlayer) viewer).canSeeOthers);
        });

        events.addListener(PlayerDisconnectEvent.class, e -> {
            ParkourUtil.despawnCubes((ParkourPlayer) e.getPlayer());
            PlaceHandler.playerBlocks.remove(e.getPlayer().getUuid());
        });

        events.addListener(PlayerSwapItemEvent.class, e -> {
            if (!MapData.portal) {
                return;
            }

            e.setCancelled(true);

            ParkourPlayer player = (ParkourPlayer) e.getPlayer();

            Entity lookingAt = null;

            if (player.currentlyHolding != null) {
                lookingAt = instance.getEntityByUuid(player.currentlyHolding);
            } else {
                Vec rayStart = player.getPosition().asVec().add(new Vec(0, player.getEyeHeight(), 0));
                Vec rayDir = player.getPosition().direction();

                for (Entity entity : instance.getNearbyEntities(player.getPosition(), 5f)) {
                    if (entity.getEntityType() == EntityType.BLOCK_DISPLAY && player.getUuid().equals(entity.getTag(CubeEntity.CUBE_OWNER)) &&
                            entity.getBoundingBox().boundingBoxRayIntersectionCheck(rayStart, rayDir, entity.getPosition())) {
                        lookingAt = entity;
                        break;
                    }
                }
            }

            if (lookingAt != null) {
                ((CubeEntity) lookingAt).interact();
            }
        });

        events.addListener(PlayerTickEvent.class, e -> {
            ParkourPlayer player = (ParkourPlayer) e.getPlayer();

            long time = player.calculateTimeSpent(System.currentTimeMillis());
            player.sendActionBar(Component.text(ChatUtil.formatTime(time, player.finishTime != 0)));
        });

        PlaceHandler.setup(instance);

        events.addListener(ItemDropEvent.class, e -> e.setCancelled(true));
        events.addListener(PlayerBlockBreakEvent.class, e -> e.setCancelled(true));

        events.addListener(PlayerMoveEvent.class, e -> MovementHandler.handlePlayerMoveEvent(e, this));

        events.addListener(PlayerUseItemEvent.class, e -> {
            e.setCancelled(true);
            ItemHandler.handlePlayerUseItemEvent((ParkourPlayer) e.getPlayer(), e.getHand(), e.getItemStack().material());
        });

        Log.logger().info("Started Parkour game");
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                new SignFeature(),
                new PlayerHeadFeature(),
                new AdminCommandsFeature(),
                new PacketDebugFeature(),
                new NoCollisionFeature()
        );
    }

    public record Config(String worldName) { }
}
