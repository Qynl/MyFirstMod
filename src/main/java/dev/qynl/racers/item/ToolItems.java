package dev.qynl.racers.item;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.car.PaintColors;
import dev.qynl.racers.car.Tune;
import dev.qynl.racers.net.ModNetworking;
import dev.qynl.racers.registry.ModItems;
import dev.qynl.racers.stats.StatsManager;
import dev.qynl.racers.track.TrackManager;
import dev.qynl.racers.util.Cars;
import dev.qynl.racers.util.Stopwatch;

/** The toolbox: wrench, paint can, track wand, stopwatch, trophies, key and parts. */
public final class ToolItems {
    private ToolItems() {
    }

    private static CarEntity carAt(World world, PlayerEntity player) {
        if (player.getVehicle() instanceof CarEntity car) {
            return car;
        }
        CarEntity near = Cars.carNear(world, player.getPos(), 4.0);
        return near;
    }

    /** Turns a plain action result from a helper into an item-use result. */
    static TypedActionResult<ItemStack> wrap(ActionResult result, ItemStack stack) {
        if (result == ActionResult.FAIL) {
            return TypedActionResult.fail(stack);
        }
        if (result == ActionResult.PASS) {
            return TypedActionResult.pass(stack);
        }
        return TypedActionResult.success(stack);
    }

    /** Repairs a car, picks it back up, or opens the tuning sheet. */
    public static class Wrench extends Item {
        public Wrench(Settings settings) {
            super(settings);
        }

        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            if (context.getWorld().isClient) {
                return ActionResult.SUCCESS;
            }
            CarEntity car = Cars.carNear(context.getWorld(), context.getBlockPos().toCenterPos(), 3.0);
            if (car == null) {
                return ActionResult.PASS;
            }
            return interact(car, context.getPlayer(), context.getStack(), context.getPlayer().isSneaking());
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient) {
                return TypedActionResult.success(stack);
            }
            CarEntity car = carAt(world, user);
            if (car == null) {
                user.sendMessage(Text.translatable("message.fabricracers.wrench_none")
                        .formatted(Formatting.GRAY), true);
                return TypedActionResult.pass(stack);
            }
            return wrap(interact(car, user, stack, user.isSneaking()), stack);
        }

        private ActionResult interact(CarEntity car, PlayerEntity player, ItemStack wrench, boolean sneaking) {
            if (player == null) {
                return ActionResult.PASS;
            }
            if (sneaking) {
                if (!car.data().ownedBy(player)) {
                    player.sendMessage(Text.translatable("message.fabricracers.not_your_car",
                            Text.literal(car.ownerName())).formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
                if (car.isRidden()) {
                    player.sendMessage(Text.translatable("message.fabricracers.car_occupied")
                            .formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
                ItemStack stack = new ItemStack(ModItems.carItem(car.data().type));
                CarData.apply(stack, car.data().withHealth(1.0F, 1.0F));
                car.discard();
                if (!player.getInventory().insertStack(stack)) {
                    player.dropItem(stack, false);
                }
                if (!player.isCreative()) {
                    wrench.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
                }
                player.sendMessage(Text.translatable("message.fabricracers.car_picked_up")
                        .formatted(Formatting.GREEN), true);
                return ActionResult.CONSUME;
            }
            if (player.isSneaking() || !car.data().ownedBy(player)) {
                return car.repair(player, wrench);
            }
            // open the tuning sheet on the client
            if (player instanceof ServerPlayerEntity serverPlayer) {
                net.minecraft.nbt.NbtCompound extra = new net.minecraft.nbt.NbtCompound();
                for (Tune tune : Tune.values()) {
                    extra.putInt(tune.id, dev.qynl.racers.shop.PartShop.costFor(tune,
                            car.data().level(tune) + 1));
                }
                ModNetworking.sendGarage(serverPlayer, car, extra);
            }
            return ActionResult.CONSUME;
        }
    }

    /** Claims an ownerless car, or repaints one. */
    public static class CarKey extends Item {
        public CarKey(Settings settings) {
            super(settings);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient) {
                return TypedActionResult.success(stack);
            }
            CarEntity car = carAt(world, user);
            if (car == null) {
                user.sendMessage(Text.translatable("message.fabricracers.wrench_none")
                        .formatted(Formatting.GRAY), true);
                return TypedActionResult.pass(stack);
            }
            if (car.ownerName().isEmpty() || car.owner() == null) {
                car.setOwner(user.getUuid(), user.getName().getString());
                user.sendMessage(Text.translatable("message.fabricracers.claimed",
                        car.carType().displayName).formatted(Formatting.GREEN), true);
                return TypedActionResult.success(stack);
            }
            user.sendMessage(Text.translatable("message.fabricracers.key_owner",
                    Text.literal(car.ownerName())).formatted(Formatting.YELLOW), true);
            return TypedActionResult.success(stack);
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            tooltip.add(Text.translatable("item.fabricracers.carrot_key.desc").formatted(Formatting.GRAY));
        }
    }

    /** Repaints a car. */
    public static class PaintCan extends Item {
        private final int color;

        public PaintCan(int color, Settings settings) {
            super(settings);
            this.color = color;
        }

        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            if (context.getWorld().isClient) {
                return ActionResult.SUCCESS;
            }
            CarEntity car = Cars.carNear(context.getWorld(), context.getBlockPos().toCenterPos(), 3.0);
            return paint(car, context.getPlayer(), context.getStack());
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient) {
                return TypedActionResult.success(stack);
            }
            return wrap(paint(carAt(world, user), user, stack), stack);
        }

        private ActionResult paint(CarEntity car, PlayerEntity player, ItemStack stack) {
            if (car == null || player == null) {
                return ActionResult.PASS;
            }
            if (!car.data().ownedBy(player)) {
                player.sendMessage(Text.translatable("message.fabricracers.not_your_car",
                        Text.literal(car.ownerName())).formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            car.setPaint(color, car.data().secondaryColor);
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            player.sendMessage(Text.translatable("message.fabricracers.painted",
                    Text.literal(PaintColors.nameOf(color)).formatted(PaintColors.chat(color)))
                    .formatted(Formatting.GRAY), true);
            return ActionResult.CONSUME;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            tooltip.add(Text.translatable("item.fabricracers.paint.desc").formatted(Formatting.GRAY));
        }
    }

    /** Selects and places whole circuits. */
    public static class TrackWand extends Item {
        public TrackWand(Settings settings) {
            super(settings);
        }

        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            return use(context.getWorld(), context.getPlayer(), context.getHand(), context.getBlockPos());
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            BlockPos pos = user == null ? BlockPos.ORIGIN : user.getBlockPos();
            return wrap(use(world, user, hand, pos), stack);
        }

        private ActionResult use(World world, PlayerEntity user, Hand hand, BlockPos pos) {
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }
            if (!(user instanceof ServerPlayerEntity player)) {
                return ActionResult.PASS;
            }
            TrackManager manager = TrackManager.get(player.server);
            if (!player.isCreativeLevelTwoOp() && !manager.placements().isEmpty()
                    && player.getVehicle() == null) {
                // anyone may build in creative, otherwise it is an op tool
                player.sendMessage(Text.translatable("message.fabricracers.wand_needs_op")
                        .formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            BlockPos anchor = player.isSneaking() && !manager.placements().isEmpty()
                    ? player.getBlockPos() : pos.offset(player.getHorizontalFacing());
            manager.markWandAnchor(player, anchor);
            ModNetworking.sendTrackList(player, manager, anchor);
            return ActionResult.CONSUME;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            tooltip.add(Text.translatable("item.fabricracers.track_wand.desc").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("item.fabricracers.track_wand.desc2").formatted(Formatting.DARK_GRAY));
        }
    }

    /** Lap stopwatch (client side timer). */
    public static class Stopwatch extends Item {
        public Stopwatch(Settings settings) {
            super(settings);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient) {
                dev.qynl.racers.util.Stopwatch.items().toggle();
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.success(stack);
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            tooltip.add(Text.translatable("item.fabricracers.stopwatch.desc").formatted(Formatting.GRAY));
        }
    }

    /** Bragging rights; printing your racing record. */
    public static class Trophy extends Item {
        private final String place;

        public Trophy(String place, Settings settings) {
            super(settings);
            this.place = place;
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient) {
                return TypedActionResult.success(stack);
            }
            if (user instanceof ServerPlayerEntity player) {
                StatsManager.get(player.server).sendRecord(player);
            }
            return TypedActionResult.success(stack);
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            tooltip.add(Text.translatable("item.fabricracers.trophy_" + place + ".desc")
                    .formatted(Formatting.GRAY));
        }
    }

    /** One upgrade part, consumed by the garage. */
    public static class TunePart extends Item {
        private final Tune tune;

        public TunePart(Tune tune, Settings settings) {
            super(settings);
            this.tune = tune;
        }

        public Tune tune() {
            return tune;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            tooltip.add(Text.translatable("tooltip.fabricracers.tune_part", tune.description)
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.fabricracers.tune_part_use")
                    .formatted(Formatting.DARK_GRAY));
        }
    }

    /** Race entry token: right click to see the state of the local race. */
    public static class RacePass extends Item {
        public RacePass(Settings settings) {
            super(settings);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient) {
                return TypedActionResult.success(stack);
            }
            if (user instanceof ServerPlayerEntity player) {
                var session = dev.qynl.racers.race.RaceManager.get(player.server).sessionFor(player);
                if (session == null) {
                    player.sendMessage(Text.translatable("race.fabricracers.pass_free")
                            .formatted(Formatting.GRAY), true);
                } else {
                    player.sendMessage(Text.literal(session.standingsText()).formatted(Formatting.WHITE),
                            false);
                }
            }
            return TypedActionResult.success(stack);
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            tooltip.add(Text.translatable("item.fabricracers.race_pass.desc").formatted(Formatting.GRAY));
        }
    }
}
