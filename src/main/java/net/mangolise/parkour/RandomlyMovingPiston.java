package net.mangolise.parkour;

import net.mangolise.gamesdk.block.MoveablePiston;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.Instance;

import java.util.concurrent.ThreadLocalRandom;

public class RandomlyMovingPiston extends MoveablePiston {
    private int timer;

    public RandomlyMovingPiston(Instance instance, Vec pos, String facing) {
        super(instance, pos, facing);

        timer = ThreadLocalRandom.current().nextInt(0, 120);

        instance.eventNode().addListener(InstanceTickEvent.class, e -> tick());
    }

    public void tick() {
        if (timer > 0) {
            timer--;
            return;
        }

        if (isOpen()) {
            close();
            timer = ThreadLocalRandom.current().nextInt(160, 320);
        }
        else {
            open();
            timer = 10;
        }
    }
}
