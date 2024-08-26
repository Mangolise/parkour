package net.mangolise.parkour;

import com.google.gson.*;
import net.mangolise.parkour.element.*;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.io.IOException;
import java.util.*;

public class MapData {
    public static List<List<Pos>> checkpoints = new ArrayList<>();
    public static List<ItemPickup> itemPickups = new ArrayList<>();
    public static Set<Material> pickupMaterials = new HashSet<>();
    public static List<RandomlyMovingPiston> pistons = new ArrayList<>();
    public static List<Vec> cubeSpawns = new ArrayList<>();
    public static List<Plate> plates = new ArrayList<>();
    public static List<Door> doors = new ArrayList<>();
    public static boolean portal = false;
    public static int deathLevel = 0;

    public static void loadMapData(Instance instance, String worldName) throws IOException {
        String stringJson = new String(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(
                "worlds/" + worldName + ".json")).readAllBytes());

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(stringJson, JsonObject.class);

        deathLevel = getIntOrDefault(root, "deathLevel", 0);

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

        for (JsonElement raw : getArrayOrEmpty(root, "itemPickups")) {
            String[] split = raw.getAsString().split("\\|");
            if (split.length < 3) {
                throw new IllegalArgumentException("not enough args for blockItems");
            }

            List<Vec> pos = getMultiVec(split[0]);
            Material mat = Material.fromNamespaceId("minecraft:" + split[1]);
            int count = Integer.parseInt(split[2]);

            if (mat == null) {
                throw new IllegalArgumentException("Material '" + split[1] + "' doesnt exist!");
            }

            itemPickups.add(new ItemPickup(pos, ItemStack.of(mat, count)));
            pickupMaterials.add(mat);
        }

        JsonObject portal = root.getAsJsonObject("portal");
        if (portal != null) {
            MapData.portal = true;

            for (JsonElement rawPos : getArrayOrEmpty(portal, "cubes")) {
                String[] strPos = rawPos.getAsString().split(" ");
                cubeSpawns.add(getVecFromSplit(strPos));
            }

            for (JsonElement rawPos : getArrayOrEmpty(portal, "doors")) {
                List<Vec> pos = getMultiVec(rawPos.getAsString());
                doors.add(new Door(instance, pos));
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

                plates.add(new Plate(instance, getVecFromSplit(strPos), targets));
            }
        }
    }

    private static int getIntOrDefault(JsonObject object, String memberName, int defaultValue) {
        return Objects.requireNonNullElseGet(object.get(memberName), () -> new JsonPrimitive(defaultValue)).getAsInt();
    }

    private static List<Vec> getMultiVec(String positionsString) {
        String[] positions = positionsString.split(",");

        List<Vec> pos = new ArrayList<>(positions.length);
        for (String position : positions) {
            pos.add(getVecFromSplit(position.split(" ")));
        }

        return pos;
    }

    private static Vec getVecFromSplit(String[] strPos) {
        if (strPos.length < 3) {
            throw new IllegalArgumentException("coordinate without x, y, and z");
        }

        return new Vec(Double.parseDouble(strPos[0]), Double.parseDouble(strPos[1]), Double.parseDouble(strPos[2]));
    }

    private static Iterable<JsonElement> getArrayOrEmpty(JsonObject object, String path) {
        return Objects.requireNonNullElseGet(object.getAsJsonArray(path), List::of);
    }
}
