package net.mangolise.parkour;

import net.mangolise.gamesdk.util.Util;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extras.bungee.BungeeCordProxy;

import java.util.ArrayList;

// This is a dev server, not used in production
public class Test {
    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        MinecraftServer.getConnectionManager().setUuidProvider((connection, username) -> Util.createFakeUUID(username));

        if (Util.useBungeeCord()) {
            BungeeCordProxy.enable();
        }

        server.start("0.0.0.0", Util.getConfiguredPort());

        ParkourGame.Config config = new ParkourGame.Config("nether", new ArrayList<>());
        ParkourGame game = new ParkourGame(config);
        game.setup();
    }
}
