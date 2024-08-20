package net.mangolise.parkour;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.GameModeCommandFeature;
import net.mangolise.gamesdk.features.SignFeature;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.util.Util;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
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

    public final MapData mapData = new MapData(new ArrayList<>());
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
            String stringJson = new String(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(
                    "worlds/" + config.worldName + ".json")).readAllBytes());

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(stringJson, JsonObject.class);

            for (JsonElement rawPoss : root.getAsJsonArray("checkpoints")) {
                String[] strPoss = rawPoss.getAsString().split("\\|");
                List<Pos> poss = new ArrayList<>();

                for (String rawPos : strPoss) {
                    String[] strPos = rawPos.split(" ");

                    if (strPos.length < 3) {
                        throw new IllegalArgumentException("Position without x y and z");
                    }

                    Pos pos = new Pos(Double.parseDouble(strPos[0]) + 0.5, Double.parseDouble(strPos[1]) + 0.5,
                            Double.parseDouble(strPos[2]) + 0.5);

                    if (strPos.length >= 4) {
                        pos = pos.withYaw(Float.parseFloat(strPos[3]));
                    }

                    if (strPos.length >= 5) {
                        pos = pos.withPitch(Float.parseFloat(strPos[4]));
                    }

                    poss.add(pos);
                }

                mapData.checkpoints.add(poss);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Player spawning
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            Player player = e.getPlayer();
            e.setSpawningInstance(instance);

            player.setRespawnPoint(mapData.checkpoints.getFirst().getFirst());
            player.setGameMode(GameMode.ADVENTURE);
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(-128);
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE).setBaseValue(-128);
        });

        events.addListener(PlayerSpawnEvent.class, e -> {
            Player player = e.getPlayer();

            ItemHandler.giveGameItems(player);
            ParkourUtil.resetPlayer(player, mapData);
            player.updateViewableRule(viewer -> viewer.getTag(CAN_SEE_OTHERS_TAG));
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
    public record MapData(List<List<Pos>> checkpoints) {
        public List<List<Pos>> getCheckpoints() {
            return checkpoints;
        }
    }
}
