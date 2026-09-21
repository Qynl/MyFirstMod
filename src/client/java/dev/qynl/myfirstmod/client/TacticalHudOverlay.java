package dev.qynl.myfirstmod.client;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.item.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TacticalHudOverlay implements HudRenderCallback {
    private static final int COLOR_CARD_BG = 0xd5070c18;
    private static final int COLOR_CARD_BORDER = 0xff334155;
    private static final int COLOR_BAR_BG = 0xff0f172a;

    public static void register() {
        HudRenderCallback.EVENT.register(new TacticalHudOverlay());
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || client.options.hudHidden) {
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        TextRenderer textRenderer = client.textRenderer;

        // 1. Target Unit Scanner Dossier (Top-Center)
        renderTargetScanner(context, client, screenWidth, textRenderer);

        // 2. Commander Weapon & Command Palette HUD (Bottom-Left)
        renderCommanderPalette(context, client, screenHeight, textRenderer);
    }

    private void renderTargetScanner(DrawContext context, MinecraftClient client, int screenWidth, TextRenderer textRenderer) {
        if (!(client.targetedEntity instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }

        String factionId = UnitSystem.getTagValue(target, "faction:");
        String unitId = UnitSystem.getTagValue(target, "unit:");
        String role = UnitSystem.getTagValue(target, "role:");

        int cardW = 210;
        int cardH = 44;
        int cardX = (screenWidth - cardW) / 2;
        int cardY = 12;

        int accentColor = getFactionColor(factionId);

        // Backdrop panel
        context.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_CARD_BG);
        // Top accent line
        context.fill(cardX, cardY, cardX + cardW, cardY + 2, accentColor);
        // Left accent stripe
        context.fill(cardX, cardY, cardX + 3, cardY + cardH, accentColor);

        // Unit Name & Faction Title
        String name = target.getCustomName() != null ? target.getCustomName().getString() : target.getName().getString();
        if (unitId != null && !unitId.isBlank()) {
            name = formatUnitName(unitId);
        }

        String factionDisplay = getFactionDisplayName(factionId);
        context.drawText(textRenderer, Text.literal(factionDisplay).formatted(Formatting.BOLD), cardX + 8, cardY + 5, accentColor, false);
        context.drawText(textRenderer, Text.literal(name).formatted(Formatting.WHITE), cardX + 8, cardY + 15, 0xffffffff, false);

        // Health Bar
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float hpPercent = Math.max(0.0f, Math.min(1.0f, health / Math.max(1.0f, maxHealth)));

        int barX = cardX + 8;
        int barY = cardY + 26;
        int barW = cardW - 16;
        int barH = 5;

        int hpColor = hpPercent > 0.5f ? 0xff10b981 : (hpPercent > 0.25f ? 0xfff59e0b : 0xffef4444);

        context.fill(barX, barY, barX + barW, barY + barH, COLOR_BAR_BG);
        context.fill(barX, barY, barX + (int) (barW * hpPercent), barY + barH, hpColor);

        // Numeric HP & Role info
        String hpText = String.format("%.1f/%.0f HP", health, maxHealth);
        context.drawText(textRenderer, Text.literal(hpText).formatted(Formatting.GRAY), barX, cardY + 33, 0xff94a3b8, false);

        if (role != null && !role.isBlank()) {
            String roleText = "[" + role.toUpperCase() + "]";
            int roleW = textRenderer.getWidth(roleText);
            context.drawText(textRenderer, Text.literal(roleText).formatted(Formatting.AQUA), cardX + cardW - 8 - roleW, cardY + 33, 0xff38bdf8, false);
        } else {
            var dmgAttr = target.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (dmgAttr != null) {
                String dmgText = String.format("🗡 %.1f", dmgAttr.getValue());
                int dmgW = textRenderer.getWidth(dmgText);
                context.drawText(textRenderer, Text.literal(dmgText).formatted(Formatting.GOLD), cardX + cardW - 8 - dmgW, cardY + 33, 0xfff59e0b, false);
            }
        }
    }

    private void renderCommanderPalette(DrawContext context, MinecraftClient client, int screenHeight, TextRenderer textRenderer) {
        if (client.player == null) return;

        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();

        ItemStack tool = isCommanderItem(mainHand) ? mainHand : (isCommanderItem(offHand) ? offHand : null);
        if (tool == null) return;

        int boxW = 190;
        int boxH = 46;
        int boxX = 8;
        int boxY = screenHeight - boxH - 8;

        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, COLOR_CARD_BG);
        context.fill(boxX, boxY, boxX + boxW, boxY + 2, 0xfff59e0b); // Gold top border
        context.fill(boxX, boxY, boxX + 3, boxY + boxH, 0xfff59e0b);

        context.drawText(textRenderer, Text.literal("🎖️ COMMANDER'S TACTICAL PALETTE").formatted(Formatting.GOLD, Formatting.BOLD), boxX + 8, boxY + 5, 0xfff59e0b, false);
        context.drawText(textRenderer, Text.literal("• Tool: " + tool.getName().getString()).formatted(Formatting.WHITE), boxX + 8, boxY + 16, 0xfff8fafc, false);
        context.drawText(textRenderer, Text.literal("• [R-Click]: Deploy / Command").formatted(Formatting.GRAY), boxX + 8, boxY + 26, 0xffcbd5e1, false);
        context.drawText(textRenderer, Text.literal("• [Shift+R-Click]: Cycle Options").formatted(Formatting.DARK_GRAY), boxX + 8, boxY + 35, 0xff94a3b8, false);
    }

    private boolean isCommanderItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof UnitCreatorItem
                || item instanceof TacticalWhistleItem
                || item instanceof CommanderHornItem
                || item instanceof FactionScepterItem
                || item instanceof FactionBannerItem
                || item instanceof PaladinMaceItem
                || item instanceof HealingStaffItem
                || item instanceof DruidStaffItem
                || item instanceof BardLuteItem
                || item instanceof BombardierMortarItem
                || item instanceof AssassinDaggerItem
                || item instanceof ReinforcementBeaconItem
                || item instanceof TransmutationWandItem;
    }

    private int getFactionColor(String factionId) {
        if (factionId == null) return 0xff94a3b8;
        return switch (factionId.toLowerCase()) {
            case "kingdom" -> 0xff3b82f6;
            case "raiders" -> 0xffef4444;
            case "villagers" -> 0xff10b981;
            case "undead" -> 0xff8b5cf6;
            case "arcane" -> 0xff06b6d4;
            default -> 0xffeab308;
        };
    }

    private String getFactionDisplayName(String factionId) {
        if (factionId == null) return "🏳 Neutral";
        return switch (factionId.toLowerCase()) {
            case "kingdom" -> "🔵 Kingdom of Eldoria";
            case "raiders" -> "🔴 Iron Raiders";
            case "villagers" -> "🟢 Village Alliance";
            case "undead" -> "🟣 Undead Legion";
            case "arcane" -> "🔷 Arcane Order";
            default -> "🚩 " + factionId.toUpperCase();
        };
    }

    private String formatUnitName(String unitId) {
        String clean = unitId.replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : clean.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
