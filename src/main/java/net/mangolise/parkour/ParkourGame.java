package net.mangolise.parkour;

import net.kyori.adventure.text.Component;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.GameModeCommandFeature;
import net.mangolise.gamesdk.features.PlayerHeadFeature;
import net.mangolise.gamesdk.features.SignFeature;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.parkour.command.CheckpointCommand;
import net.mangolise.parkour.element.CubeEntity;
import net.mangolise.parkour.handler.ItemHandler;
import net.mangolise.parkour.handler.MovementHandler;
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
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.util.*;

public class ParkourGame extends BaseGame<ParkourGame.Config> {
    public static final Tag<Integer> CURRENT_CHECKPOINT_TAG = Tag.Integer("current_checkpoint").defaultValue(0);
    public static final Tag<Boolean> CAN_JUMPPAD_TAG = Tag.Boolean("can_jump_pad").defaultValue(true);
    public static final Tag<Boolean> CAN_SEE_OTHERS_TAG = Tag.Boolean("can_see_others").defaultValue(true);
    public static final Tag<Integer> DEATH_COUNT_TAG = Tag.Integer("death_count").defaultValue(0);
    public static final Tag<Long> START_TIME_TAG = Tag.Long("start_time");
    public static final Tag<Long> FINISH_TIME_TAG = Tag.Long("finish_time");
    public static @UnknownNullability ParkourGame game;

    public final Map<UUID, List<CubeEntity>> cubes = new HashMap<>();
    public @UnknownNullability MapData mapData;
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
            mapData = new MapData(instance, config.worldName);
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
            e.getPlayer().setGameMode(GameMode.ADVENTURE);
        });

        events.addListener(PlayerSpawnEvent.class, e -> {
            Player player = e.getPlayer();

            // init stuff
            player.setRespawnPoint(MapData.checkpoints.getFirst().getFirst());
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(-128);
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(-128);

            ItemHandler.giveGameItems(player);
            ParkourUtil.resetPlayer(player);
            player.updateViewableRule(viewer -> viewer.getTag(CAN_SEE_OTHERS_TAG));
            player.setTeam(team);

            // cube
            if (!MapData.cubeSpawns.isEmpty()) {
                List<CubeEntity> cubeList = new ArrayList<>();
                for (Vec pos : MapData.cubeSpawns) {
                    cubeList.add(new CubeEntity(game.instance, player, pos));
                }

                cubes.put(player.getUuid(), cubeList);
            }
        });

        events.addListener(PlayerDisconnectEvent.class, e -> {
            if (!MapData.cubeSpawns.isEmpty()) {
                UUID uuid = e.getPlayer().getUuid();
                for (CubeEntity cube : cubes.get(uuid)) {
                    cube.remove();
                }

                cubes.remove(uuid);
            }
        });

        events.addListener(PlayerSwapItemEvent.class, e -> {
            if (!MapData.portal) {
                return;
            }

            e.setCancelled(true);

            Player player = e.getPlayer();
//            Entity lookingAt = ParkourUtil.getTarget(player, instance.getNearbyEntities(player.getPosition(), 5f));

            Entity lookingAt = null;

            if (player.hasTag(CubeEntity.CURRENTLY_HOLDING)) {
                lookingAt = instance.getEntityByUuid(player.getTag(CubeEntity.CURRENTLY_HOLDING));
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
            Player p = e.getPlayer();

            if (!p.hasTag(START_TIME_TAG)) {
                return;
            }

            Long finishTime = Objects.requireNonNullElse(p.getTag(FINISH_TIME_TAG),
                    System.currentTimeMillis() - p.getTag(START_TIME_TAG));
            p.sendActionBar(Component.text(ParkourUtil.formatTime(finishTime)));
        });

        events.addListener(ItemDropEvent.class, e -> e.setCancelled(true));
        events.addListener(PlayerMoveEvent.class, e -> MovementHandler.handlePlayerMoveEvent(e, this));
        events.addListener(PlayerUseItemEvent.class, e -> ItemHandler.handlePlayerUseItemEvent(e, this));

        Log.logger().info("Started Parkour game");
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                new SignFeature(),
                new PlayerHeadFeature(),
                new GameModeCommandFeature()
        );
    }

    public record PlayerConfig(UUID uuid, String name, long bestTime) { }
    public record Config(String worldName, List<PlayerConfig> records) { }
}
