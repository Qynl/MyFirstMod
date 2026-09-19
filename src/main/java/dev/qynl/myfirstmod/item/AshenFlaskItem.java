package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.pilgrimage.PilgrimageRules;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import dev.qynl.myfirstmod.realm.Waystones;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Charges belong to the player, not the stack: crafting/swapping flasks cannot refill them. */
public final class AshenFlaskItem extends Item {
    public AshenFlaskItem(Settings settings) {super(settings);}
    public static boolean safeRest(ServerPlayerEntity player) {
        var world=player.getServerWorld();
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM) || Waystones.combatLocked(player)
                || player.hurtTime>0 || !player.isOnGround()) return false;
        if(!world.getEntitiesByClass(HostileEntity.class,player.getBoundingBox().expand(12),e->e.isAlive()).isEmpty()) return false;
        for(BlockPos pos:BlockPos.iterate(player.getBlockPos().add(-3,-2,-3),player.getBlockPos().add(3,2,3)))
            if(world.isChunkLoaded(pos) && world.getBlockState(pos).isOf(ModBlocks.WAYSTONE)) return true;
        return false;
    }
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        var stack=player.getStackInHand(hand);
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM) || player.getItemCooldownManager().isCoolingDown(this))
            return TypedActionResult.fail(stack);
        if(player instanceof ServerPlayerEntity server) {
            var record=RealmState.get(server.getServerWorld()).expedition(player.getUuid());
            boolean rest=player.isSneaking();
            if(rest ? !safeRest(server) : record.flaskCharges<=0 || player.getHealth()>=player.getMaxHealth()) {
                player.sendMessage(Text.translatable(rest?"message.myfirstmod.rest_unsafe":"message.myfirstmod.flask_empty"),true);
                return TypedActionResult.fail(stack);
            }
            var data=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
            data.putBoolean("AshenRest",rest);stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(data));
        }
        player.setCurrentHand(hand);return TypedActionResult.consume(stack);
    }
    public static boolean resting(ItemStack stack) {return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getBoolean("AshenRest");}
    @Override public int getMaxUseTime(ItemStack stack,LivingEntity user) {return resting(stack)?100:32;}
    @Override public UseAction getUseAction(ItemStack stack) {return UseAction.DRINK;}
    @Override public void usageTick(World world,LivingEntity user,ItemStack stack,int remaining) {
        if(!(user instanceof ServerPlayerEntity player)) return;
        if(player.hurtTime>0 || resting(stack) && (!player.isSneaking() || !safeRest(player))) {player.stopUsingItem();return;}
        if(remaining%10==0) player.getServerWorld().spawnParticles(ParticleTypes.SOUL,player.getX(),player.getY()+.6,player.getZ(),5,.3,.5,.3,.015);
    }
    @Override public ItemStack finishUsing(ItemStack stack,World world,LivingEntity user) {
        if(!(user instanceof ServerPlayerEntity player) || player.hurtTime>0 || !world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)) return stack;
        var state=RealmState.get(player.getServerWorld());var record=state.expedition(player.getUuid());
        if(resting(stack)) {
            if(!player.isSneaking() || !safeRest(player)) return stack;
            var ember=player.getOffHandStack();
            if(ember.isOf(ModItems.MOURNING_EMBER) && record.flaskUpgrades<2) {
                record.flaskUpgrades++;if(!player.isCreative()) ember.decrement(1);
                player.sendMessage(Text.translatable("message.myfirstmod.flask_upgraded",PilgrimageRules.capacity(record.flaskUpgrades)),false);
            }
            record.flaskCharges=PilgrimageRules.capacity(record.flaskUpgrades);player.setHealth(player.getMaxHealth());
            player.sendMessage(Text.translatable("message.myfirstmod.rested"),true);
        } else {
            if(record.flaskCharges<=0) return stack;
            record.flaskCharges--;player.heal(8);player.getItemCooldownManager().set(this,80);
        }
        state.markDirty();sync(player);player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,1,.7f);
        return stack;
    }
    public static void tick(MinecraftServer server) {
        for(var player:server.getPlayerManager().getPlayerList()) if(player.age%20==0) sync(player);
    }
    private static void sync(ServerPlayerEntity player) {
        var realm=player.getServer().getWorld(VoidPortalManager.NULL_REALM);if(realm==null) return;
        var record=RealmState.get(realm).expedition(player.getUuid());
        for(int i=0;i<player.getInventory().size();i++) {
            var stack=player.getInventory().getStack(i);if(!stack.isOf(ModItems.ASHEN_FLASK)) continue;
            var data=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
            int max=PilgrimageRules.capacity(record.flaskUpgrades);
            if(data.getInt("AshenCharges")==record.flaskCharges && data.getInt("AshenCapacity")==max) continue;
            data.putInt("AshenCharges",record.flaskCharges);data.putInt("AshenCapacity",max);
            stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(data));
        }
    }
}
