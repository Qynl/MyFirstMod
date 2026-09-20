package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.gui.CreatorScreenHandler;
import dev.qynl.myfirstmod.unit.UnitSystem;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class UnitCreatorItem extends Item {
    public UnitCreatorItem(Settings settings){super(settings.maxCount(1));}
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand){ if(!world.isClient) player.openHandledScreen(new SimpleNamedScreenHandlerFactory((sync,inv,p)->new CreatorScreenHandler(sync,inv), Text.translatable("screen.myfirstmod.creator"))); return TypedActionResult.success(player.getStackInHand(hand),world.isClient()); }
    @Override public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, net.minecraft.entity.LivingEntity entity, Hand hand){ return ActionResult.PASS; }
    public static boolean spawnEquipped(PlayerEntity player){ if(player.getServer()==null)return false; UnitWorldData d=UnitWorldData.get(player.getServer()); String id=d.equipped.getOrDefault(player.getUuid(),d.firstUnit()); var u=d.units.get(id); return u!=null && UnitSystem.spawn((net.minecraft.server.network.ServerPlayerEntity)player,u); }
}
