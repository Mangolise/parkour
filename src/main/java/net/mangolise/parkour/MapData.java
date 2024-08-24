package net.mangolise.parkour;

import com.google.gson.*;
import net.mangolise.parkour.element.ActivatableElement;
import net.mangolise.parkour.element.Door;
import net.mangolise.parkour.element.Plate;
import net.mangolise.parkour.element.RandomlyMovingPiston;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapData {
    public static List<List<Pos>> checkpoints = new ArrayList<>();
    public static List<RandomlyMovingPiston> pistons = new ArrayList<>();
    public static List<Vec> cubeSpawns = new ArrayList<>();
    public static List<Plate> plates = new ArrayList<>();
    public static List<Door> doors = new ArrayList<>();
    public static boolean portal = false;

    public MapData(Instance instance, String worldName) throws IOException {
        String stringJson = new String(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(
                "worlds/" + worldName + ".json")).readAllBytes());

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(stringJson, JsonObject.class);

        for (JsonElement rawPoss : root.getAsJsonArray("checkpoints")) {
            String[] strPoss = rawPoss.getAsString().split(",");
            List<Pos> poss = new ArrayList<>();

            for (String rawPos : strPoss) {
                String[] strPos = rawPos.split(" ");
                Pos pos = getVecFromSplit(strPos).add(0.5, 0.0, 0.5).asPosition();

                if (strPos.length >= 4) {
                    pos = pos.withYaw(Float.parseFloat(strPos[3]));
                }

                if (strPos.length >= 5) {
                    pos = pos.withPitch(Float.parseFloat(strPos[4]));
                }

                poss.add(pos);
            }

            checkpoints.add(poss);
        }

        for (JsonElement rawPos : getArrayOrEmpty(root, "pistons")) {
            String[] strPos = rawPos.getAsString().split(" ");
            Vec pos = getVecFromSplit(strPos);

            if (strPos.length < 4) {
                throw new IllegalArgumentException("piston without facing dir");
            }

            pistons.add(new RandomlyMovingPiston(instance, pos, strPos[3]));
        }

        JsonObject portal = root.getAsJsonObject("portal");
        if (portal != null) {
            MapData.portal = true;

            for (JsonElement rawPos : getArrayOrEmpty(portal, "cubes")) {
                String[] strPos = rawPos.getAsString().split(" ");
                cubeSpawns.add(getVecFromSplit(strPos));
            }

            for (JsonElement rawPos : getArrayOrEmpty(portal, "doors")) {
                String[] positions = rawPos.getAsString().split(",");

                List<Vec> pos = new ArrayList<>(positions.length);
                for (String position : positions) {
                    String[] strPos = position.split(" ");
                    pos.add(getVecFromSplit(strPos));
                }

                doors.add(new Door(ParkourGame.game.instance, pos));
            }

            for (JsonElement rawPos : getArrayOrEmpty(portal, "plates")) {
                String[] posEqu = rawPos.getAsString().split("=");
                String[] strPos = posEqu[0].split(" ");

                String[] results = posEqu[1].split(",");
                List<ActivatableElement> targets = new ArrayList<>(results.length);
                for (String result : results) {
                    String[] resplit = result.split(" ");
                    if (resplit[0].equals("door")) {
                        targets.add(doors.get(Integer.parseInt(resplit[1])));
                    }
                }

                plates.add(new Plate(ParkourGame.game.instance, getVecFromSplit(strPos), targets));
            }
        }
    }

    private Vec getVecFromSplit(String[] strPos) {
        if (strPos.length < 3) {
            throw new IllegalArgumentException("coordinate without x, y, and z");
        }

        return new Vec(Double.parseDouble(strPos[0]), Double.parseDouble(strPos[1]), Double.parseDouble(strPos[2]));
    }

    private JsonArray getArrayOrEmpty(JsonObject object, String path) {
        return Objects.requireNonNullElseGet(object.getAsJsonArray(path), JsonArray::new);
    }
}
