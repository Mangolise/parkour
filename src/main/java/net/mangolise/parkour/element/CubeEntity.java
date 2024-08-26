package net.mangolise.parkour.element;

import net.mangolise.parkour.MapData;
import net.mangolise.parkour.ParkourUtil;
import net.mangolise.parkour.PlayerData;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;

import java.util.UUID;

public class CubeEntity extends Entity {
    public static Tag<UUID> CUBE_OWNER = Tag.UUID("cube_owner");

    private final Entity childEntity;
    private final LivingEntity shulker;

    private final Player owner;
    private boolean held = false;

    public CubeEntity(Instance instance, Player owner, Vec pos) {
        super(EntityType.BLOCK_DISPLAY);
        this.owner = owner;

        setBoundingBox(new BoundingBox(1d, 1d, 1d, Vec.ZERO));
        editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(Block.EMERALD_BLOCK);
            meta.setTransformationInterpolationDuration(1);
            meta.setPosRotInterpolationDuration(1);
        });

        setTag(CUBE_OWNER, owner.getUuid());
        setViewableRule(this);

        // Child entity
        childEntity = new Entity(EntityType.BLOCK_DISPLAY);
        setViewableRule(childEntity);
        childEntity.setBoundingBox(new BoundingBox(1d, 1d, 1d, new Vec(-0.5, 0.0, -0.5)));
        childEntity.setInstance(instance);
        childEntity.teleport(pos.asPosition());
        childEntity.setNoGravity(true);
        childEntity.editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(Block.AIR);
            meta.setTransformationInterpolationDuration(1);
            meta.setPosRotInterpolationDuration(1);
        });

        // Shulker
        shulker = new LivingEntity(EntityType.SHULKER);
        setViewableRule(shulker);
        shulker.setInstance(instance);
        shulker.teleport(pos.asPosition());
        shulker.setInvisible(true);
        childEntity.addPassenger(shulker);

        setInstance(instance);
        teleport(pos.asPosition());
    }

    private void setViewableRule(Entity entity) {
        entity.updateViewableRule(viewer -> viewer.getUuid().equals(getTag(CUBE_OWNER)));
    }

    public void interact() {
        held = !held;
        PlayerData playerData = ParkourUtil.getData(owner);

        if (held) {
            playerData.currentlyHolding = getUuid();
            setNoGravity(true);

        } else {
            playerData.currentlyHolding = null;
            setNoGravity(false);
        }
    }

    @Override
    public void tick(long time) {
        for (Door door : MapData.doors) {
            door.setBlocks(owner, false);
        }

        super.tick(time);

        if (held) {
            Vec viewOffset = new Vec(0.5, -0.5, 0.5).rotateFromView(owner.getPosition());
            Vec direction = owner.getPosition().withPitch(pitch -> Math.min(pitch, 48d)).direction();

            Vec targetPos = owner.getPosition().asVec()
                    .add(0d, owner.getEyeHeight(), 0d)
                    .add(direction.mul(2))
                    .add(owner.getPosition().sub(owner.getPreviousPosition()).withY(0).mul(4))
                    .add(viewOffset)
                    .sub(0.5);

            Vec diff = targetPos.sub(getPosition().add(viewOffset));
            if (diff.length() > 5) {
                interact();
            }

            setVelocity(diff.mul(6));
        }

        childEntity.teleport(getPosition().add(0.5, 0.0, 0.5));
        childEntity.setVelocity(Vec.ZERO);

        for (Door door : MapData.doors) {
            door.setBlocks(owner, true);
        }
    }

    @Override
    public void remove() {
        shulker.remove();
        childEntity.remove();

        super.remove();
    }
}
