package net.mangolise.parkour;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapData {
    public List<List<Pos>> checkpoints = new ArrayList<>();
    public List<RandomlyMovingPiston> pistons = new ArrayList<>();

    public MapData(Instance instance, String worldName) throws IOException {
        String stringJson = new String(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(
                "worlds/" + worldName + ".json")).readAllBytes());

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(stringJson, JsonObject.class);

        for (JsonElement rawPoss : root.getAsJsonArray("checkpoints")) {
            String[] strPoss = rawPoss.getAsString().split("\\|");
            List<Pos> poss = new ArrayList<>();

            for (String rawPos : strPoss) {
                String[] strPos = rawPos.split(" ");

                if (strPos.length < 3) {
                    throw new IllegalArgumentException("checkpoint without x y and z");
                }

                Pos pos = new Pos(Double.parseDouble(strPos[0]) + 0.5, Double.parseDouble(strPos[1]),
                        Double.parseDouble(strPos[2]) + 0.5);

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

        for (JsonElement rawPos : root.getAsJsonArray("pistons")) {
            String[] strPos = rawPos.getAsString().split(" ");

            if (strPos.length < 3) {
                throw new IllegalArgumentException("piston without x y z and facing dir");
            }

            Vec pos = new Vec(Double.parseDouble(strPos[0]), Double.parseDouble(strPos[1]), Double.parseDouble(strPos[2]));

            pistons.add(new RandomlyMovingPiston(instance, pos, strPos[3]));
        }
    }
}
