package dev.qynl.myfirstmod.unit;

import dev.qynl.myfirstmod.ai.UnitSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.SkeletonHorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class MountedCavalrySpawner {
    private MountedCavalrySpawner() {}

    public static LivingEntity spawnMounted(ServerWorld world, UnitDefinition riderDef, Identifier mountTypeId, Vec3d pos, float yaw) {
        if (world == null || riderDef == null || mountTypeId == null) return null;

        EntityType<?> mountType = Registries.ENTITY_TYPE.getOrEmpty(mountTypeId).orElse(null);
        if (mountType == null) return UnitSpawner.spawnDirect(world, riderDef, pos, yaw);

        Entity mountEntity = mountType.create(world);
        if (!(mountEntity instanceof MobEntity mount)) {
            return UnitSpawner.spawnDirect(world, riderDef, pos, yaw);
        }

        mount.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);

        if (mount instanceof HorseEntity horse) {
            horse.setTame(true);
            horse.equipStack(EquipmentSlot.BODY, new ItemStack(Items.DIAMOND_HORSE_ARMOR));
            horse.equipStack(EquipmentSlot.FEET, new ItemStack(Items.SADDLE));
        } else if (mount instanceof SkeletonHorseEntity skelHorse) {
            skelHorse.setTame(true);
            skelHorse.equipStack(EquipmentSlot.FEET, new ItemStack(Items.SADDLE));
        }

        mount.getCommandTags().add("faction:" + riderDef.factionId);
        mount.getCommandTags().add("cavalry_mount");
        mount.getCommandTags().add("battle_mob");
        world.spawnEntity(mount);

        LivingEntity rider = UnitSpawner.spawnDirect(world, riderDef, pos, yaw);
        if (rider != null) {
            rider.startRiding(mount, true);
        }

        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1.0, pos.z, 15, 0.4, 0.6, 0.4, 0.05);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_HORSE_GALLOP.value(), SoundCategory.NEUTRAL, 1.0f, 1.0f);

        return rider;
    }
}
