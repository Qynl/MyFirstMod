package dev.qynl.myfirstmod.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class VeteranProgression {
    private VeteranProgression() {}

    public static void recordKillForUnit(ServerWorld world, LivingEntity unit) {
        if (unit == null || !unit.isAlive()) return;

        int kills = UnitSystem.getNumericTag(unit, "kills:") + 1;
        unit.getCommandTags().removeIf(t -> t.startsWith("kills:"));
        unit.getCommandTags().add("kills:" + kills);

        // Check for promotion thresholds
        if (kills == 3) {
            promote(world, unit, "Veteran", Formatting.GOLD, 4.0, 1.0);
        } else if (kills == 7) {
            promote(world, unit, "Elite Champion", Formatting.AQUA, 8.0, 2.0);
            unit.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 99999, 0, false, false));
        } else if (kills == 15) {
            promote(world, unit, "Legendary Hero", Formatting.LIGHT_PURPLE, 15.0, 3.5);
            unit.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 99999, 0, false, false));
            unit.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 99999, 0, false, false));
        }
    }

    private static void promote(ServerWorld world, LivingEntity unit, String title, Formatting format, double hpBonus, double dmgBonus) {
        EntityAttributeInstance maxHp = unit.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(maxHp.getBaseValue() + hpBonus);
            unit.heal((float) hpBonus);
        }

        EntityAttributeInstance dmg = unit.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.setBaseValue(dmg.getBaseValue() + dmgBonus);
        }

        Text currentName = unit.getCustomName();
        if (currentName != null) {
            Text newName = currentName.copy().append(Text.literal(" [" + title + "]").setStyle(Style.EMPTY.withColor(format).withBold(true)));
            unit.setCustomName(newName);
        }

        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, unit.getX(), unit.getY() + 1.0, unit.getZ(), 25, 0.4, 0.6, 0.4, 0.1);
        world.playSound(null, unit.getX(), unit.getY(), unit.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }
}
