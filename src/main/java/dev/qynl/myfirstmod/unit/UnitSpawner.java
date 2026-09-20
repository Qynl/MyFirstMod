package dev.qynl.myfirstmod.unit;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionPerk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class UnitSpawner {
    private UnitSpawner() {}

    public static LivingEntity spawn(ServerWorld world, UnitDefinition unit, Vec3d pos, float yaw) {
        if (world == null || unit == null || unit.entityId == null) return null;

        // Check if mounted cavalry
        if (unit.mount != null && !unit.mount.isEmpty() && !unit.mount.equalsIgnoreCase("none")) {
            Identifier mountId = Identifier.tryParse(unit.mount);
            if (mountId != null) {
                return MountedCavalrySpawner.spawnMounted(world, unit, mountId, pos, yaw);
            }
        }

        return spawnDirect(world, unit, pos, yaw);
    }

    public static LivingEntity spawnDirect(ServerWorld world, UnitDefinition unit, Vec3d pos, float yaw) {
        if (world == null || unit == null || unit.entityId == null) return null;

        EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(unit.entityId).orElse(null);
        if (type == null) return null;

        Entity entity = type.create(world);
        if (!(entity instanceof LivingEntity living)) return null;

        living.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
        living.setHeadYaw(yaw);
        living.setBodyYaw(yaw);

        // Apply base definition (attributes, scale, equipment, inventory, variants)
        unit.apply(living);

        // Prevent Piglin/Piglin Brute zombification in the Overworld
        if (living instanceof AbstractPiglinEntity piglin) {
            piglin.setImmuneToZombification(true);
        }

        // Apply faction perks
        applyFactionPerks(world, unit, living);

        // Format custom name with faction color
        applyCustomName(world, unit, living);

        // Attach command tags for high-speed AI queries
        living.getCommandTags().add("unit:" + unit.id);
        living.getCommandTags().add("faction:" + unit.factionId);
        living.getCommandTags().add("role:" + unit.role.toLowerCase());
        living.getCommandTags().add("rank:" + unit.rank.toLowerCase());
        if (unit.squad != null && !unit.squad.isBlank()) {
            living.getCommandTags().add("squad:" + unit.squad);
        }
        if (unit.commander) {
            living.getCommandTags().add("commander");
        }
        if (unit.infiniteAmmo) {
            living.getCommandTags().add("infinite_ammo");
        }
        if (unit.particleAura != null && !unit.particleAura.equalsIgnoreCase("none")) {
            living.getCommandTags().add("aura:" + unit.particleAura.toLowerCase());
        }
        if (unit.deathAction != null && !unit.deathAction.equalsIgnoreCase("none")) {
            living.getCommandTags().add("death:" + unit.deathAction.toLowerCase());
        }

        // Count ammunition in inventory
        int ammoCount = unit.inventory.stream()
                .filter(s -> s.isOf(Items.ARROW) || s.isOf(Items.SPECTRAL_ARROW) || s.isOf(Items.TIPPED_ARROW))
                .mapToInt(ItemStack::getCount)
                .sum();
        living.getCommandTags().add("ammo:" + ammoCount);

        // Count fireworks
        int fireworkCount = unit.inventory.stream()
                .filter(s -> s.isOf(Items.FIREWORK_ROCKET))
                .mapToInt(ItemStack::getCount)
                .sum();
        if (unit.equipment.getOrDefault(EquipmentSlot.OFFHAND, ItemStack.EMPTY).isOf(Items.FIREWORK_ROCKET)) {
            fireworkCount += unit.equipment.get(EquipmentSlot.OFFHAND).getCount();
        }
        living.getCommandTags().add("fireworks:" + fireworkCount);

        if (living instanceof MobEntity mob) {
            mob.setPersistent();
        }

        world.spawnEntity(living);

        // Visual and auditory spawn effects
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.8, pos.z, 12, 0.4, 0.5, 0.4, 0.05);
        world.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.5, pos.z, 15, 0.3, 0.4, 0.3, 0.1);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.NEUTRAL, 0.75f, 1.4f);

        return living;
    }

    public static boolean spawnAtPlayer(ServerPlayerEntity player, UnitDefinition unit) {
        if (player == null || unit == null) return false;
        Vec3d look = player.getRotationVector();
        Vec3d spawnPos = player.getPos().add(look.x * 2.5, 0.0, look.z * 2.5);
        return spawn(player.getServerWorld(), unit, spawnPos, player.getYaw()) != null;
    }

    public static boolean spawnSquadAtPlayer(ServerPlayerEntity player, UnitDefinition unit, int count) {
        if (player == null || unit == null || count <= 0) return false;
        ServerWorld world = player.getServerWorld();
        Vec3d center = player.getPos().add(player.getRotationVector().multiply(3.0));
        float yaw = player.getYaw();

        for (int i = 0; i < count; i++) {
            double offsetX = (i % 3 - 1) * 1.5;
            double offsetZ = (i / 3) * 1.5;
            Vec3d spawnPos = center.add(offsetX, 0.0, offsetZ);
            spawn(world, unit, spawnPos, yaw);
        }
        return true;
    }

    private static void applyFactionPerks(ServerWorld world, UnitDefinition unit, LivingEntity living) {
        if (world.getServer() == null) return;
        Faction faction = UnitWorldData.get(world.getServer()).factions.get(unit.factionId);
        if (faction == null) return;

        if (faction.hasPerk(FactionPerk.MILITARY_DISCIPLINE)) {
            EntityAttributeInstance dmg = living.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() * 1.15);
        }

        if (faction.hasPerk(FactionPerk.HEAVY_ARMOR)) {
            EntityAttributeInstance armor = living.getAttributeInstance(EntityAttributes.ARMOR);
            if (armor != null) armor.setBaseValue(armor.getBaseValue() * 1.20);
            EntityAttributeInstance kb = living.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
            if (kb != null) kb.setBaseValue(kb.getBaseValue() + 0.2);
        }

        if (faction.hasPerk(FactionPerk.SWIFT_ARMY)) {
            EntityAttributeInstance speed = living.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.15);
        }

        if (faction.hasPerk(FactionPerk.FORTIFICATION)) {
            EntityAttributeInstance health = living.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (health != null) health.setBaseValue(health.getBaseValue() * 1.15);
            living.setHealth(living.getMaxHealth());
        }
    }

    private static void applyCustomName(ServerWorld world, UnitDefinition unit, LivingEntity living) {
        if (world.getServer() == null) return;
        Faction faction = UnitWorldData.get(world.getServer()).factions.get(unit.factionId);
        int colorRgb = faction != null ? faction.getParsedColor() : 0x94A3B8;

        String factionTag = faction != null ? "[" + faction.name + "] " : "";
        String roleTag = unit.commander ? " ★ " : " ";

        Text customName = Text.literal(factionTag)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb)).withBold(true))
                .append(Text.literal(unit.name + roleTag).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false)));

        living.setCustomName(customName);
        living.setCustomNameVisible(true);
    }
}
