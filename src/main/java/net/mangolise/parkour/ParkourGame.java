package net.mangolise.parkour;

import net.kyori.adventure.text.Component;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.GameModeCommandFeature;
import net.mangolise.gamesdk.features.SignFeature;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.util.Util;
import net.minestom.server.MinecraftServer;
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

    public @UnknownNullability MapData mapData;
    public @UnknownNullability Instance instance;

    public ParkourGame(Config config) {
        super(config);
    }

    @Override
    public void setup() {
        super.setup();
        instance = MinecraftServer.getInstanceManager().createInstanceContainer(
                Util.getPolarLoaderFromResource("worlds/" + config.worldName + ".polar"));
        instance.enableAutoChunkLoad(true);

        // Load MapData
        try {
            mapData = new MapData(instance, config.worldName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Team team = MinecraftServer.getTeamManager().createTeam("all");
        team.setCollisionRule(TeamsPacket.CollisionRule.NEVER);

        // Player spawning
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setGameMode(GameMode.ADVENTURE);
        });

        events.addListener(PlayerSpawnEvent.class, e -> {
            Player player = e.getPlayer();

            player.setRespawnPoint(mapData.checkpoints.getFirst().getFirst());
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(-128);
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(-128);

            ItemHandler.giveGameItems(player);
            ParkourUtil.resetPlayer(player, mapData);
            player.updateViewableRule(viewer -> viewer.getTag(CAN_SEE_OTHERS_TAG));
            player.setTeam(team);
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
                new GameModeCommandFeature()
        );
    }

    public record PlayerConfig(UUID uuid, String name, long bestTime) { }
    public record Config(String worldName, List<PlayerConfig> records) { }
}
