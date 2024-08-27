package net.mangolise.parkour;

import net.kyori.adventure.text.Component;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.AdminCommandsFeature;
import net.mangolise.gamesdk.features.PacketDebugFeature;
import net.mangolise.gamesdk.features.PlayerHeadFeature;
import net.mangolise.gamesdk.features.SignFeature;
import net.mangolise.gamesdk.log.Log;
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
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
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

        Team team = MinecraftServer.getTeamManager().createTeam("all");
        team.setCollisionRule(TeamsPacket.CollisionRule.NEVER);

        MinecraftServer.getCommandManager().register(new CheckpointCommand());

        // Player spawning
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setGameMode(MapData.itemPickups.isEmpty() ? GameMode.ADVENTURE : GameMode.SURVIVAL);
        });

        events.addListener(PlayerSpawnEvent.class, e -> {
            Player player = e.getPlayer();

            PlaceHandler.playerBlocks.put(player.getUuid(), new ArrayList<>());

            // init stuff
            player.setRespawnPoint(MapData.checkpoints.getFirst().getFirst());
            player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED).setBaseValue(-128);
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(-128);

            ItemHandler.giveGameItems(player);
            ParkourUtil.resetPlayer(player);
            player.updateViewableRule(viewer -> ParkourUtil.getData(viewer).canSeeOthers);
            player.setTeam(team);
        });

        events.addListener(PlayerDisconnectEvent.class, e -> {
            ParkourUtil.despawnCubes(e.getPlayer());
            PlaceHandler.playerBlocks.remove(e.getPlayer().getUuid());
        });

        events.addListener(PlayerSwapItemEvent.class, e -> {
            if (!MapData.portal) {
                return;
            }

            e.setCancelled(true);

            Player player = e.getPlayer();
            PlayerData playerData = ParkourUtil.getData(player);

            Entity lookingAt = null;

            if (playerData.currentlyHolding != null) {
                lookingAt = instance.getEntityByUuid(playerData.currentlyHolding);
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
            Player player = e.getPlayer();
            PlayerData playerData = ParkourUtil.getData(player);

            if (playerData.startTime == 0) {
                return;
            }

            long finishTime = playerData.finishTime == 0 ? System.currentTimeMillis() - playerData.startTime : playerData.finishTime;
            player.sendActionBar(Component.text(ParkourUtil.formatTime(finishTime)));
        });

        PlaceHandler.setup(instance);

        events.addListener(ItemDropEvent.class, e -> e.setCancelled(true));
        events.addListener(PlayerBlockBreakEvent.class, e -> e.setCancelled(true));

        events.addListener(PlayerMoveEvent.class, e -> MovementHandler.handlePlayerMoveEvent(e, this));

        events.addListener(PlayerUseItemOnBlockEvent.class, e -> ItemHandler.handlePlayerUseItemEvent(e.getPlayer(), e.getHand(), e.getItemStack().material()));
        events.addListener(PlayerUseItemEvent.class, e -> {
            e.setCancelled(true);
            ItemHandler.handlePlayerUseItemEvent(e.getPlayer(), e.getHand(), e.getItemStack().material());
        });

        Log.logger().info("Started Parkour game");
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                new SignFeature(),
                new PlayerHeadFeature(),
                new AdminCommandsFeature(),
                new PacketDebugFeature()
        );
    }

    public record PlayerConfig(UUID uuid, String name, long bestTime) { }
    public record Config(String worldName, List<PlayerConfig> records) { }
}
