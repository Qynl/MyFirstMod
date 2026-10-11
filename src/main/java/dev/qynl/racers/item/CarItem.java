package dev.qynl.racers.item;

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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.car.CarType;
import dev.qynl.racers.car.PlayerCarEntity;
import dev.qynl.racers.util.Cars;

import java.util.List;

/** The car itself, as an item: place it, hop in, drive it. */
public class CarItem extends Item {
    private final CarType type;

    public CarItem(CarType type, Settings settings) {
        super(settings);
        this.type = type;
    }

    public CarType carType() {
        return type;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        Vec3d spawn = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        // face away from the clicked face
        Direction side = context.getSide();
        float yaw = switch (side) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> 270.0F;
            default -> context.getPlayer() == null ? 0.0F : context.getPlayer().getYaw();
        };
        return spawnCar(context.getStack(), context.getPlayer(), world, spawn, yaw);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        // Drop it roughly a car length in front of the player
        Vec3d look = Vec3d.fromPolar(0.0F, user.getYaw()).multiply(2.6);
        Vec3d spawn = user.getPos().add(look);
        double ground = Cars.groundHeight(world, spawn);
        if (Double.isNaN(ground)) {
            user.sendMessage(Text.translatable("message.fabricracers.no_room").formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }
        ActionResult result = spawnCar(stack, user, world, new Vec3d(spawn.x, ground, spawn.z),
                user.getYaw());
        if (result == ActionResult.FAIL) {
            return TypedActionResult.fail(stack);
        }
        if (result == ActionResult.PASS) {
            return TypedActionResult.pass(stack);
        }
        return TypedActionResult.success(stack);
    }

    private ActionResult spawnCar(ItemStack stack, PlayerEntity player, World world, Vec3d spawn, float yaw) {
        CarData data = CarData.of(stack);
        if (data == null) {
            data = CarData.fresh(type, player);
            CarData.apply(stack, data);
        }
        if (player != null && !data.ownedBy(player)) {
            player.sendMessage(Text.translatable("message.fabricracers.not_your_car",
                    Text.literal(data.ownerName)).formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        PlayerCarEntity car = new PlayerCarEntity(dev.qynl.racers.registry.ModEntities.PLAYER_CAR, world);
        car.initFromData(data);
        car.refreshPositionAndAngles(spawn.x, Cars.snapCarY(world, spawn), spawn.z, yaw, 0.0F);
        if (!world.isSpaceEmpty(car, car.getBoundingBox()) || !Cars.groundIsSolid(world, car.getBlockPos())) {
            if (player != null) {
                player.sendMessage(Text.translatable("message.fabricracers.no_room").formatted(Formatting.RED), true);
                car.discard();
            }
            return ActionResult.FAIL;
        }
        world.spawnEntity(car);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            dev.qynl.racers.stats.StatsManager.get(serverPlayer.server)
                    .get(serverPlayer).carsPlaced++;
        }
        if (player == null || !player.isCreative()) {
            stack.decrement(1);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType tooltipType) {
        CarData data = CarData.of(stack);
        tooltip.add(Text.translatable("item.fabricracers." + type.itemId() + ".desc")
                .formatted(Formatting.GRAY));
        if (data != null) {
            tooltip.add(Text.translatable("tooltip.fabricracers.stats",
                    String.format("%.0f", data.stats().topSpeedKmh()),
                    String.format("%.1f", data.stats().power()),
                    data.ownerName == null || data.ownerName.isEmpty() ? "-" : data.ownerName)
                    .formatted(Formatting.DARK_GRAY));
            for (dev.qynl.racers.car.Tune tune : dev.qynl.racers.car.Tune.values()) {
                int level = data.level(tune);
                if (level > 0) {
                    tooltip.add(Text.literal(" " + tune.displayName + " " + level + "/" + tune.maxLevel)
                            .formatted(Formatting.GOLD));
                }
            }
            tooltip.add(Text.translatable("tooltip.fabricracers.color",
                    dev.qynl.racers.car.PaintColors.nameOf(data.primaryColor),
                    dev.qynl.racers.car.PaintColors.nameOf(data.secondaryColor))
                    .formatted(Formatting.DARK_GRAY));
        }
    }
}
