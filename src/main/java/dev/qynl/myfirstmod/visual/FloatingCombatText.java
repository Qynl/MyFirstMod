package dev.qynl.myfirstmod.visual;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class FloatingCombatText {
    public static final String FLOATING_TEXT_TAG = "myfirstmod:combat_text";
    private static final String SPAWN_TICK_TAG = "myfirstmod:spawn_tick:";

    private FloatingCombatText() {}

    /**
     * Spawns a floating combat damage number in 3D world space.
     */
    public static void spawnDamage(ServerWorld world, double x, double y, double z, float amount, boolean isCritical) {
        if (world == null || amount <= 0.1f) return;

        String formatted = String.format("%.1f", amount);
        Text text;
        if (isCritical) {
            text = Text.literal("⚡ -" + formatted).formatted(Formatting.GOLD, Formatting.BOLD);
            world.spawnParticles(ParticleTypes.CRIT, x, y + 0.5, z, 12, 0.3, 0.3, 0.3, 0.15);
        } else {
            text = Text.literal("-" + formatted).formatted(Formatting.RED);
        }

        spawnTextDisplay(world, x, y + 0.8, z, text, 20);
    }

    /**
     * Spawns a floating healing popup in 3D world space.
     */
    public static void spawnHeal(ServerWorld world, double x, double y, double z, float amount) {
        if (world == null || amount <= 0.1f) return;

        String formatted = String.format("%.1f", amount);
        Text text = Text.literal("+" + formatted + " ❤").formatted(Formatting.GREEN, Formatting.BOLD);
        world.spawnParticles(ParticleTypes.HEART, x, y + 0.6, z, 4, 0.2, 0.2, 0.2, 0.05);

        spawnTextDisplay(world, x, y + 0.9, z, text, 22);
    }

    /**
     * Spawns a floating tactical combat status message (e.g. BLOCKED, ROOTED, SMITE).
     */
    public static void spawnStatus(ServerWorld world, double x, double y, double z, String message, Formatting formatting) {
        if (world == null) return;
        Text text = Text.literal(message).formatted(formatting, Formatting.BOLD);
        spawnTextDisplay(world, x, y + 1.1, z, text, 24);
    }

    private static void spawnTextDisplay(ServerWorld world, double x, double y, double z, Text text, int lifetimeTicks) {
        try {
            DisplayEntity.TextDisplayEntity display = EntityType.TEXT_DISPLAY.create(world);
            if (display != null) {
                display.setPosition(x + (world.random.nextDouble() - 0.5) * 0.4, y, z + (world.random.nextDouble() - 0.5) * 0.4);
                display.setText(text);
                display.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
                display.setBackground(0x60000000); // Sleek translucent dark backing
                display.setNoGravity(true);
                display.addCommandTag(FLOATING_TEXT_TAG);
                display.addCommandTag(SPAWN_TICK_TAG + (world.getTime() + lifetimeTicks));
                world.spawnEntity(display);
            }
        } catch (Exception ignored) {
            // Fallback gracefully if display entities are constrained
        }
    }

    /**
     * Ticked by server world to animate and clean up floating combat texts.
     */
    public static void tickFloatingTexts(ServerWorld world) {
        if (world == null || world.getTime() % 2 != 0) return;

        long currentTime = world.getTime();
        var entities = world.getEntitiesByType(EntityType.TEXT_DISPLAY, e -> e.getCommandTags().contains(FLOATING_TEXT_TAG));

        for (var entity : entities) {
            // Drift gently upward
            entity.setPosition(entity.getX(), entity.getY() + 0.035, entity.getZ());

            // Check expiration
            for (String tag : entity.getCommandTags()) {
                if (tag.startsWith(SPAWN_TICK_TAG)) {
                    try {
                        long expireTime = Long.parseLong(tag.substring(SPAWN_TICK_TAG.length()));
                        if (currentTime >= expireTime) {
                            entity.discard();
                        }
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        }
    }
}
