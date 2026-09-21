package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraft.util.Hand;

import java.util.List;

public class TransmutationWandItem extends Item {
    public TransmutationWandItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
        if (!player.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer && !(entity instanceof PlayerEntity)) {
            ServerWorld world = serverPlayer.getServerWorld();
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());

            String equippedId = data.getEquippedUnit(player.getUuid());
            UnitDefinition template = data.units.get(equippedId);
            if (template == null) template = data.units.get(data.firstUnit());

            if (template != null) {
                // Apply unit template to clicked entity
                template.apply(entity);

                // Add command tags
                entity.getCommandTags().removeIf(t -> t.startsWith("unit:") || t.startsWith("faction:") || t.startsWith("role:"));
                entity.getCommandTags().add("unit:" + template.id);
                entity.getCommandTags().add("faction:" + template.factionId);
                entity.getCommandTags().add("role:" + template.role.toLowerCase());
                entity.getCommandTags().add("rank:" + template.rank.toLowerCase());
                if (!template.squad.isBlank()) entity.getCommandTags().add("squad:" + template.squad);
                if (template.commander) entity.getCommandTags().add("commander");

                Faction faction = data.factions.get(template.factionId);
                int colorRgb = faction != null ? faction.getParsedColor() : 0x3B82F6;
                String fName = faction != null ? faction.name : "Faction";

                Text newName = Text.literal("[" + fName + "] ")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb)).withBold(true))
                        .append(Text.literal(template.name).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

                entity.setCustomName(newName);
                entity.setCustomNameVisible(true);

                if (entity instanceof MobEntity mob) {
                    mob.setPersistent();
                }

                // Recruitment visual effect
                dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnOrbitalLightPillar(world, entity.getX(), entity.getY(), entity.getZ(), 4.0, ParticleTypes.TOTEM_OF_UNDYING, ParticleTypes.ENCHANTED_HIT);
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, entity.getX(), entity.getY() + 1.2, entity.getZ(), "✨ RECRUITED!", Formatting.GOLD);
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 30, 0.4, 0.6, 0.4, 0.15);
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, entity.getX(), entity.getY() + 0.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.4f);

                serverPlayer.sendMessage(
                        Text.literal("✨ Unit Recruited: ").formatted(Formatting.GOLD, Formatting.BOLD)
                                .append(Text.literal(entity.getName().getString()).formatted(Formatting.WHITE))
                                .append(Text.literal(" enlisted into ").formatted(Formatting.GRAY))
                                .append(Text.literal(fName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb))))
                                .append(Text.literal(" as " + template.name + "!").formatted(Formatting.GREEN)),
                        false
                );

                player.getItemCooldownManager().set(this, 20);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Unit Recruitment & Polymorph Wand").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click on Mob: ").formatted(Formatting.YELLOW).append(Text.literal("Recruit/Transmute mob into equipped template").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Applies gear, faction, role, and battle AI").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
