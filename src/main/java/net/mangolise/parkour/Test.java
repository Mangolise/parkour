package net.mangolise.parkour;

import net.mangolise.gamesdk.permissions.Permissions;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.extras.bungee.BungeeCordProxy;

// This is a dev server, not used in production
public class Test {
    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        if (GameSdkUtils.useBungeeCord()) {
            BungeeCordProxy.enable();
        }

        // give every permission to every player
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e ->
                Permissions.setPermission(e.getPlayer(), "*", true));

        server.start("0.0.0.0", GameSdkUtils.getConfiguredPort());

        ParkourGame.Config config = new ParkourGame.Config(System.getenv("MAP"));
        ParkourGame game = new ParkourGame(config);
        game.setup();
    }
}
