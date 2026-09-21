package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommanderHornItem extends Item {
    public enum OrderMode {
        RALLY("Rally to Commander", Formatting.YELLOW),
        CHARGE("All-Out Charge", Formatting.RED),
        HOLD("Hold Position & Defend", Formatting.AQUA);

        public final String name;
        public final Formatting format;

        OrderMode(String name, Formatting format) {
            this.name = name;
            this.format = format;
        }

        public OrderMode next() {
            return switch (this) {
                case RALLY -> CHARGE;
                case CHARGE -> HOLD;
                case HOLD -> RALLY;
            };
        }
    }

    private static final Map<UUID, OrderMode> PLAYER_ORDERS = new ConcurrentHashMap<>();

    public CommanderHornItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = serverPlayer.getServerWorld();
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());
            UUID uuid = player.getUuid();

            OrderMode mode = PLAYER_ORDERS.getOrDefault(uuid, OrderMode.RALLY);

            if (player.isSneaking()) {
                mode = mode.next();
                PLAYER_ORDERS.put(uuid, mode);
                serverPlayer.sendMessage(
                        Text.literal("🎺 Order Mode: ").formatted(Formatting.GRAY)
                                .append(Text.literal(mode.name).formatted(mode.format, Formatting.BOLD)),
                        true // Action bar
                );
                serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.2f, 1.8f);
                return TypedActionResult.success(stack, false);
            }

            String equippedUnitId = data.getEquippedUnit(player.getUuid());
            var unitDef = data.units.get(equippedUnitId);
            String factionId = unitDef != null ? unitDef.factionId : "kingdom";
            Faction faction = data.factions.get(factionId);

            // Sound war horn
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EVENT_RAID_HORN, SoundCategory.PLAYERS, 2.0f, mode == OrderMode.CHARGE ? 1.2f : 1.0f);

            // Spawn golden acoustic rally particle shockwave rings
            for (int r = 3; r <= 15; r += 3) {
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI * i) / 20.0;
                    double px = player.getX() + Math.cos(angle) * r;
                    double pz = player.getZ() + Math.sin(angle) * r;
                    serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT, px, player.getY() + 0.3, pz, 1, 0, 0, 0, 0);
                    if (i % 2 == 0) {
                        serverWorld.spawnParticles(ParticleTypes.RAID_OMEN, px, player.getY() + 0.5, pz, 1, 0, 0, 0, 0.02);
                    }
                }
            }

            serverWorld.spawnParticles(ParticleTypes.RAID_OMEN, player.getX(), player.getY() + 1.2, player.getZ(), 25, 0.5, 0.8, 0.5, 0.08);
            serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 0.8, player.getZ(), 30, 0.8, 0.5, 0.8, 0.1);

            // Find nearby allied troops in 32 block radius
            List<MobEntity> troops = serverWorld.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(32.0),
                    e -> e.isAlive() && FactionManager.isAllied(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            for (MobEntity troop : troops) {
                troop.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
                troop.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
                troop.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0));

                if (mode == OrderMode.RALLY) {
                    troop.getNavigation().startMovingTo(player, 1.25);
                } else if (mode == OrderMode.CHARGE) {
                    LivingEntity target = serverWorld.getEntitiesByClass(LivingEntity.class, troop.getBoundingBox().expand(32.0),
                            e -> e != troop && e.isAlive() && FactionManager.isHostile(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:"))).stream().findFirst().orElse(null);
                    if (target != null) {
                        troop.setTarget(target);
                        troop.getNavigation().startMovingTo(target, 1.3);
                    }
                } else if (mode == OrderMode.HOLD) {
                    troop.getNavigation().stop();
                }

                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, troop.getX(), troop.getY() + 0.5, troop.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            }

            int colorRgb = faction != null ? faction.getParsedColor() : 0x3B82F6;
            String fName = faction != null ? faction.name : "Army";

            serverPlayer.sendMessage(
                    Text.literal("🎺 Orders Issued: ").formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal("[" + mode.name + "]").formatted(mode.format, Formatting.BOLD))
                            .append(Text.literal(" to " + troops.size() + " troops of ").formatted(Formatting.GRAY))
                            .append(Text.literal(fName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb)))),
                    true // Action bar
            );

            player.getItemCooldownManager().set(this, 60); // 3 second cooldown
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Commander's Tactical War Horn").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Rally & buff nearby allied troops").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Shift + Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Cycle Orders (Rally / Charge / Hold)").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Radius: 32 blocks  •  Cooldown: 3s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
