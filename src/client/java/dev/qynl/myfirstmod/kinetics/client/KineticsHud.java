package dev.qynl.myfirstmod.kinetics.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.kinetics.KineticsConfig;
import dev.qynl.myfirstmod.kinetics.KineticsNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The Null Kinetics HUD.
 *
 * <p>Right of the hotbar it shows, top to bottom:
 * <ul>
 *   <li>a row of ability icons with cooldown sweeps and active glows,</li>
 *   <li>a segmented energy bar,</li>
 *   <li>Flow pips with a pulsing "NULL FLOW" label when chained.</li>
 * </ul>
 */
public final class KineticsHud {
    private static final Identifier ICON_DASH =
            Identifier.of(MyFirstMod.MOD_ID, "textures/gui/kinetics/dash.png");
    private static final Identifier ICON_JUMP =
            Identifier.of(MyFirstMod.MOD_ID, "textures/gui/kinetics/jump.png");
    private static final Identifier ICON_GRAPPLE =
            Identifier.of(MyFirstMod.MOD_ID, "textures/gui/kinetics/grapple.png");
    private static final Identifier ICON_WALL =
            Identifier.of(MyFirstMod.MOD_ID, "textures/gui/kinetics/wallrun.png");
    private static final Identifier ICON_GLIDE =
            Identifier.of(MyFirstMod.MOD_ID, "textures/gui/kinetics/glide.png");
    private static final Identifier ICON_SLIDE =
            Identifier.of(MyFirstMod.MOD_ID, "textures/gui/kinetics/slide.png");

    private static final int ICON_SIZE = 16;
    private static final int ICON_STEP = 19;

    // Palette
    private static final int TEAL = 0xFF3FD9C6;
    private static final int TEAL_DIM = 0xFF1C6B62;
    private static final int CYAN_BRIGHT = 0xFF9FF7EC;
    private static final int BACKDROP = 0xB00A161C;
    private static final int BORDER = 0x803FD9C6;
    private static final int AMBER = 0xFFE8B23A;
    private static final int RED = 0xFFE85A5A;
    private static final int FLOW_COLOR = 0xFFB49AFF;

    private KineticsHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(KineticsHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!KineticsClientState.hudVisible || !KineticsConfig.get().hudEnabled) return;
        if (client.player.isSpectator()) return;

        KineticsClientState st = KineticsClientState.INSTANCE;
        if (st.abilityMask == 0) return; // whole suite disabled server-side

        int x = context.getScaledWindowWidth() / 2 + 95;
        int y = context.getScaledWindowHeight() - 33;

        RenderSystem.enableBlend();

        // ---- icons ------------------------------------------------------
        drawAbilityIcon(context, ICON_DASH, x, y,
                st.abilityEnabled(KineticsNetworking.ABILITY_DASH),
                cooldownFraction(st.dashCooldownDisplay(), KineticsConfig.get().dashCooldownTicks),
                st.dashFlashTicks > 0, false);
        drawKeyBadge(context, client, KineticsKeybinds.dashKey, x, y);

        drawAbilityIcon(context, ICON_JUMP, x + ICON_STEP, y,
                st.abilityEnabled(KineticsNetworking.ABILITY_DOUBLE_JUMP),
                0.0F, false, st.localAirJumps > 0 || st.airJumps > 0);

        drawAbilityIcon(context, ICON_GRAPPLE, x + ICON_STEP * 2, y,
                st.abilityEnabled(KineticsNetworking.ABILITY_GRAPPLE),
                0.0F, st.grappling, false);

        drawAbilityIcon(context, ICON_WALL, x + ICON_STEP * 3, y,
                st.abilityEnabled(KineticsNetworking.ABILITY_WALL_RUN),
                0.0F, st.wallRunning, false);

        drawAbilityIcon(context, ICON_GLIDE, x + ICON_STEP * 4, y,
                st.abilityEnabled(KineticsNetworking.ABILITY_GLIDE),
                0.0F, st.gliding, false);
        drawKeyBadge(context, client, KineticsKeybinds.glideKey, x + ICON_STEP * 4, y);

        drawAbilityIcon(context, ICON_SLIDE, x + ICON_STEP * 5, y,
                st.abilityEnabled(KineticsNetworking.ABILITY_SLIDE),
                cooldownFraction(st.slideCooldownDisplay(), KineticsConfig.get().slideCooldownTicks),
                st.sliding, false);

        // Air jump counter
        if (st.abilityEnabled(KineticsNetworking.ABILITY_DOUBLE_JUMP)) {
            int jumps = Math.min(st.localAirJumps, 9);
            if (jumps > 0) {
                context.drawTextWithShadow(client.textRenderer, "x" + jumps,
                        x + ICON_STEP + ICON_SIZE - 9, y + ICON_SIZE - 8, CYAN_BRIGHT);
            }
        }

        // ---- energy bar -------------------------------------------------
        int barY = y + ICON_SIZE + 5;
        int barWidth = ICON_STEP * 5 + ICON_SIZE + 4;
        int barHeight = 6;
        context.fill(x - 2, barY - 2, x + barWidth + 2, barY + barHeight + 2, BACKDROP);

        float energyFraction = st.maxEnergy > 0
                ? (float) (st.energy / st.maxEnergy) : 0.0F;
        int color = energyFraction < 0.25F
                ? (Math.sin(System.currentTimeMillis() / 90.0) > 0 ? RED : AMBER)
                : energyFraction < 0.5F ? AMBER : TEAL;

        // Segmented cells
        int cells = 12;
        int cellGap = 1;
        int cellWidth = (barWidth - cellGap * (cells - 1)) / cells;
        for (int i = 0; i < cells; i++) {
            int cx1 = x + i * (cellWidth + cellGap);
            int cx2 = cx1 + cellWidth;
            context.fill(cx1, barY, cx2, barY + barHeight, TEAL_DIM);
            float cellFraction = energyFraction * cells - i;
            if (cellFraction > 0) {
                int fill = (int) (cellWidth * Math.min(1.0F, cellFraction));
                context.fill(cx1, barY, cx1 + fill, barY + barHeight, color);
            }
        }
        context.drawBorder(x - 2, barY - 2, barWidth + 4, barHeight + 4, BORDER);

        // Numeric readout when not full
        if (energyFraction < 0.999F) {
            String label = String.valueOf((int) Math.ceil(st.energy));
            context.drawTextWithShadow(client.textRenderer, label,
                    x + barWidth + 6, barY - 1, color);
        }

        // ---- flow -------------------------------------------------------
        KineticsConfig cfg = KineticsConfig.get();
        if (st.abilityEnabled(KineticsNetworking.ABILITY_FLOW) && st.flowStacks > 0) {
            int pipY = barY + barHeight + 5;
            for (int i = 0; i < cfg.flowMaxStacks; i++) {
                int px = x + i * 8;
                boolean filled = i < st.flowStacks;
                int pipColor = filled ? FLOW_COLOR : 0x44303044;
                drawDiamond(context, px + 3, pipY + 3, 3, pipColor);
            }
            int pulse = st.flowPulseTicks > 0 ? 0x80 + (st.flowPulseTicks * 5) : 0xFF;
            int alpha = Math.max(0x40, Math.min(0xFF, pulse));
            context.drawTextWithShadow(client.textRenderer,
                    Text.translatable("hud.myfirstmod.flow", st.flowStacks),
                    x + cfg.flowMaxStacks * 8 + 6, pipY - 1,
                    (alpha << 24) | (FLOW_COLOR & 0xFFFFFF));
        }

        RenderSystem.disableBlend();
    }

    /** Draws the bound key (e.g. "V") in the bottom-right corner of an icon. */
    private static void drawKeyBadge(DrawContext context, MinecraftClient client,
                                     net.minecraft.client.option.KeyBinding key, int x, int y) {
        if (key == null) return;
        String label = key.getBoundKeyLocalizedText().getString();
        if (label.isEmpty() || label.length() > 3) return;
        int w = client.textRenderer.getWidth(label);
        context.drawTextWithShadow(client.textRenderer, label,
                x + ICON_SIZE - w - 1, y + ICON_SIZE - 8, 0xFFB8E8E0);
    }

    private static float cooldownFraction(int remaining, int total) {
        if (total <= 0 || remaining <= 0) return 0.0F;
        return Math.min(1.0F, (float) remaining / total);
    }

    private static void drawAbilityIcon(DrawContext context, Identifier icon, int x, int y,
                                        boolean enabled, float cooldown, boolean active, boolean ready) {
        float alpha = !enabled ? 0.18F : active || ready ? 1.0F : 0.42F;

        // Active glow
        if (active) {
            context.fill(x - 2, y - 2, x + ICON_SIZE + 2, y + ICON_SIZE + 2, 0x503FD9C6);
            context.drawBorder(x - 2, y - 2, ICON_SIZE + 4, ICON_SIZE + 4, CYAN_BRIGHT);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        context.drawTexture(icon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Cooldown sweep
        if (cooldown > 0.0F && enabled) {
            int sweepHeight = (int) (ICON_SIZE * cooldown);
            context.fill(x, y + ICON_SIZE - sweepHeight, x + ICON_SIZE, y + ICON_SIZE, 0xB4050A0F);
        }
    }

    private static void drawDiamond(DrawContext context, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = r - Math.abs(dy);
            context.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }
}
