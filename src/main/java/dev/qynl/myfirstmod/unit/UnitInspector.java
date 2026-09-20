package dev.qynl.myfirstmod.unit;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class UnitInspector {
    private UnitInspector() {}

    public static boolean inspectUnit(ServerPlayerEntity player, LivingEntity entity) {
        if (player == null || entity == null) return false;

        String unitId = UnitSystem.getTagValue(entity, "unit:");
        if (unitId == null) return false;

        UnitWorldData data = UnitWorldData.get(player.getServer());
        UnitDefinition unitDef = data.units.get(unitId);

        String factionId = UnitSystem.getTagValue(entity, "faction:");
        Faction faction = data.factions.get(factionId);
        int colorRgb = faction != null ? faction.getParsedColor() : 0x3B82F6;
        String fName = faction != null ? faction.name : "Independent";

        String role = UnitSystem.getTagValue(entity, "role:");
        String rank = UnitSystem.getTagValue(entity, "rank:");
        int kills = UnitSystem.getNumericTag(entity, "kills:");
        int ammo = UnitSystem.getNumericTag(entity, "ammo:");
        int fireworks = UnitSystem.getNumericTag(entity, "fireworks:");

        player.sendMessage(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GRAY), false);
        player.sendMessage(Text.literal("📋 UNIT DOSSIER: ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal(entity.getCustomName() != null ? entity.getCustomName().getString() : (unitDef != null ? unitDef.name : unitId)).formatted(Formatting.WHITE, Formatting.BOLD)), false);

        player.sendMessage(Text.literal("Faction: ").formatted(Formatting.GRAY)
                .append(Text.literal(fName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb)).withBold(true)))
                .append(Text.literal("  |  Role: ").formatted(Formatting.GRAY))
                .append(Text.literal(role != null ? role.toUpperCase() : "MELEE").formatted(Formatting.AQUA))
                .append(Text.literal("  |  Rank: ").formatted(Formatting.GRAY))
                .append(Text.literal(rank != null ? rank.toUpperCase() : "SOLDIER").formatted(Formatting.YELLOW)), false);

        float currentHp = entity.getHealth();
        float maxHp = entity.getMaxHealth();
        String hpBar = buildHealthBar(currentHp, maxHp);

        player.sendMessage(Text.literal("Health: ").formatted(Formatting.GRAY)
                .append(Text.literal(hpBar + " "))
                .append(Text.literal(String.format("%.1f / %.1f HP", currentHp, maxHp)).formatted(Formatting.GREEN)), false);

        ItemStack mainHand = entity.getEquippedStack(EquipmentSlot.MAINHAND);
        ItemStack offHand = entity.getEquippedStack(EquipmentSlot.OFFHAND);

        player.sendMessage(Text.literal("Equipment: ").formatted(Formatting.GRAY)
                .append(Text.literal("Main: ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(mainHand.isEmpty() ? "None" : mainHand.getName().getString()).formatted(Formatting.WHITE))
                .append(Text.literal("  |  Offhand: ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(offHand.isEmpty() ? "None" : offHand.getName().getString()).formatted(Formatting.WHITE)), false);

        player.sendMessage(Text.literal("Ammunition: ").formatted(Formatting.GRAY)
                .append(Text.literal(ammo + " Arrows  •  " + fireworks + " Fireworks").formatted(Formatting.YELLOW))
                .append(Text.literal("  |  Battle Kills: ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(kills)).formatted(Formatting.GOLD, Formatting.BOLD)), false);

        if (entity instanceof MobEntity mob && mob.getTarget() != null) {
            LivingEntity target = mob.getTarget();
            String tFaction = UnitSystem.getTagValue(target, "faction:");
            player.sendMessage(Text.literal("Engaging Target: ").formatted(Formatting.RED)
                    .append(Text.literal(target.getName().getString() + " [" + (tFaction != null ? tFaction : "Hostile") + "]").formatted(Formatting.WHITE)), false);
        }

        player.sendMessage(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GRAY), false);

        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.8f, 1.4f);

        return true;
    }

    private static String buildHealthBar(float current, float max) {
        int totalBars = 10;
        int filled = Math.max(0, Math.min(totalBars, Math.round((current / max) * totalBars)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filled; i++) sb.append("❤");
        return sb.toString();
    }
}
