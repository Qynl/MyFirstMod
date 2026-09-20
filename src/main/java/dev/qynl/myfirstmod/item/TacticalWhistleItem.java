package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitWorldData;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class TacticalWhistleItem extends Item {
    public enum FormationType {
        SHIELD_WALL("Shield Wall (Defensive Line)", Formatting.AQUA),
        WEDGE("Wedge (Shock Assault)", Formatting.GOLD),
        PERIMETER("Circle (Perimeter Guard)", Formatting.GREEN),
        SCATTER("Loose Spread (Anti-Artillery)", Formatting.YELLOW);

        public final String name;
        public final Formatting color;

        FormationType(String name, Formatting color) {
            this.name = name;
            this.color = color;
        }

        public FormationType next() {
            return switch (this) {
                case SHIELD_WALL -> WEDGE;
                case WEDGE -> PERIMETER;
                case PERIMETER -> SCATTER;
                case SCATTER -> SHIELD_WALL;
            };
        }
    }

    private static FormationType activeFormation = FormationType.SHIELD_WALL;

    public TacticalWhistleItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = serverPlayer.getServerWorld();
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());

            if (player.isSneaking()) {
                activeFormation = activeFormation.next();
                serverPlayer.sendMessage(
                        Text.literal("🎺 Formation Mode: ").formatted(Formatting.GRAY)
                                .append(Text.literal(activeFormation.name).formatted(activeFormation.color, Formatting.BOLD)),
                        true // Action bar
                );
                serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.2f, 1.8f);
                return TypedActionResult.success(stack, false);
            }

            // Whistle audio effect
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5f, 1.6f);

            String equippedId = data.getEquippedUnit(player.getUuid());
            var unit = data.units.get(equippedId);
            String factionId = unit != null ? unit.factionId : "kingdom";

            List<MobEntity> troops = serverWorld.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(32.0),
                    e -> e.isAlive() && FactionManager.isAllied(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            Vec3d playerPos = player.getPos();
            Vec3d forward = player.getRotationVector().normalize();
            Vec3d right = new Vec3d(-forward.z, 0, forward.x).normalize();

            int count = troops.size();
            for (int i = 0; i < count; i++) {
                MobEntity troop = troops.get(i);
                Vec3d targetSlot = playerPos;

                switch (activeFormation) {
                    case SHIELD_WALL -> {
                        double offset = (i - (count / 2.0)) * 1.6;
                        targetSlot = playerPos.add(forward.multiply(3.0)).add(right.multiply(offset));
                    }
                    case WEDGE -> {
                        int row = (i + 1) / 2;
                        int side = (i % 2 == 0) ? 1 : -1;
                        targetSlot = playerPos.add(forward.multiply(4.0 - row * 1.5)).add(right.multiply(side * row * 1.5));
                    }
                    case PERIMETER -> {
                        double angle = (2 * Math.PI * i) / Math.max(1, count);
                        targetSlot = playerPos.add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                    }
                    case SCATTER -> {
                        double angle = (2 * Math.PI * i) / Math.max(1, count);
                        targetSlot = playerPos.add(Math.cos(angle) * (6.0 + (i % 3) * 2.0), 0, Math.sin(angle) * (6.0 + (i % 3) * 2.0));
                    }
                }

                troop.getNavigation().startMovingTo(targetSlot.x, targetSlot.y, targetSlot.z, 1.25);
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, targetSlot.x, targetSlot.y + 0.2, targetSlot.z, 6, 0.2, 0.2, 0.2, 0.05);
            }

            serverPlayer.sendMessage(
                    Text.literal("⚔ Formation Executed: ").formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal(activeFormation.name).formatted(activeFormation.color, Formatting.BOLD))
                            .append(Text.literal(" (" + count + " units deployed)").formatted(Formatting.GRAY)),
                    true // Action bar
            );

            player.getItemCooldownManager().set(this, 30);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Tactical Formation Whistle").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Deploy formation layout").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Shift + Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Cycle formation mode").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Shield Wall • Wedge • Perimeter • Scatter").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
