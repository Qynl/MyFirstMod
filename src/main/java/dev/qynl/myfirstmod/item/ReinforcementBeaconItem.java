package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ReinforcementBeaconItem extends Item {
    public ReinforcementBeaconItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || context.getWorld().isClient) return ActionResult.SUCCESS;

        ServerWorld world = (ServerWorld) context.getWorld();
        BlockPos pos = context.getBlockPos().offset(context.getSide());

        if (player instanceof ServerPlayerEntity serverPlayer) {
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());
            String equippedId = data.getEquippedUnit(player.getUuid());
            var unit = data.units.get(equippedId);
            String factionId = unit != null ? unit.factionId : "kingdom";
            Faction faction = data.factions.get(factionId);

            List<UnitDefinition> factionUnits = data.units.values().stream().filter(u -> u.factionId.equalsIgnoreCase(factionId)).toList();
            if (factionUnits.isEmpty()) {
                factionUnits = data.units.values().stream().toList();
            }

            // Spawn 4 reinforcement units around target point
            Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            // Orbital beam descent particles from the clouds
            for (double dy = 0; dy <= 36; dy += 0.8) {
                world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y + dy, center.z, 3, 0.15, 0.15, 0.15, 0.01);
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + dy, center.z, 2, 0.25, 0.25, 0.25, 0.05);

                // Spiral particle vortex descending along the beam
                double spiralAngle = (dy * 0.8) % (2 * Math.PI);
                double sx = center.x + Math.cos(spiralAngle) * 0.8;
                double sz = center.z + Math.sin(spiralAngle) * 0.8;
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, sx, center.y + dy, sz, 1, 0, 0, 0, 0);
            }

            for (int i = 0; i < 4; i++) {
                UnitDefinition def = factionUnits.get(i % factionUnits.size());
                double ox = (i % 2 == 0 ? 1 : -1) * 1.8;
                double oz = (i / 2 == 0 ? 1 : -1) * 1.8;
                Vec3d sPos = center.add(ox, 0, oz);
                LivingEntity droppedUnit = UnitSpawner.spawn(world, def, sPos, serverPlayer.getYaw());
                if (droppedUnit != null) {
                    world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, sPos.x, sPos.y + 0.8, sPos.z, 15, 0.3, 0.4, 0.3, 0.08);
                }
            }

            // Epic visual landing shockwave: expanding concentric ground ring
            for (int r = 1; r <= 3; r++) {
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI * i) / 20.0;
                    double px = center.x + Math.cos(angle) * (r * 1.8);
                    double pz = center.z + Math.sin(angle) * (r * 1.8);
                    world.spawnParticles(ParticleTypes.CLOUD, px, center.y + 0.1, pz, 1, 0.1, 0.05, 0.1, 0.02);
                }
            }

            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5, center.z, 4, 0.4, 0.4, 0.4, 0.0);
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, center.x, center.y + 1.2, center.z, 80, 1.4, 1.8, 1.4, 0.25);
            world.spawnParticles(ParticleTypes.FLASH, center.x, center.y + 1.0, center.z, 3, 0.1, 0.1, 0.1, 0.0);
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.8f, 1.2f);
            world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0f, 1.0f);
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.1f, 1.4f);

            int colorRgb = faction != null ? faction.getParsedColor() : 0x3B82F6;
            String fName = faction != null ? faction.name : "Army";

            serverPlayer.sendMessage(
                    Text.literal("⚡ Orbital Reinforcements Deployed: ").formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal("4 elite units of ").formatted(Formatting.GRAY))
                            .append(Text.literal(fName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb)).withBold(true)))
                            .append(Text.literal(" dropped into combat!").formatted(Formatting.YELLOW)),
                    false
            );

            player.getItemCooldownManager().set(this, 160); // 8 second cooldown
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Reinforcement Drop Beacon").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click on Ground: ").formatted(Formatting.YELLOW).append(Text.literal("Call down 4 elite drop-pod reinforcements").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Features orbital beam VFX and instantaneous tactical deployment").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Cooldown: 8s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
