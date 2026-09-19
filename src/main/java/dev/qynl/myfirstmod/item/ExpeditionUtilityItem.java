package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.block.ModBlocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Comparator;

/** Small bounded utility actions. Scanning never generates chunks. */
public final class ExpeditionUtilityItem extends Item {
    public enum Kind { SURVEY, VEIL, REPAIR }
    private final Kind kind;
    public ExpeditionUtilityItem(Settings settings,Kind kind) {super(settings);this.kind=kind;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        ItemStack stack=user.getStackInHand(hand);
        if(user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if(world instanceof ServerWorld server) {
            if(kind==Kind.REPAIR) {
                var target=user.getStackInHand(hand==Hand.MAIN_HAND?Hand.OFF_HAND:Hand.MAIN_HAND);
                if(!target.isDamaged() || !Registries.ITEM.getId(target.getItem()).getNamespace().equals("myfirstmod")) {
                    user.sendMessage(Text.translatable("message.myfirstmod.repair_hint"),true);return TypedActionResult.fail(stack);
                }
                target.setDamage(Math.max(0,target.getDamage()-400));
                if(!user.isCreative()) stack.decrement(1);
                user.getItemCooldownManager().set(this,20);
            } else if(kind==Kind.VEIL) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,160,0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,160,0));
                user.getItemCooldownManager().set(this,900);
                stack.damage(1,user,hand==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
                server.spawnParticles(ParticleTypes.REVERSE_PORTAL,user.getX(),user.getY()+1,user.getZ(),24,.5,.5,.5,.02);
            } else {
                var found=new ArrayList<BlockPos>();
                for(BlockPos pos:BlockPos.iterate(user.getBlockPos().add(-6,-6,-6),user.getBlockPos().add(6,6,6))) {
                    if(!world.isChunkLoaded(pos)) continue;
                    var block=world.getBlockState(pos).getBlock();
                    if(block==ModBlocks.RESONITE_ORE || block==ModBlocks.PRISM_ORE || block==ModBlocks.CINDER_ORE) found.add(pos.toImmutable());
                }
                found.sort(Comparator.comparingDouble(p->p.toCenterPos().squaredDistanceTo(user.getPos())));
                if(found.isEmpty()) user.sendMessage(Text.translatable("message.myfirstmod.survey_empty"),true);
                for(int i=0;i<Math.min(3,found.size());i++) {
                    var p=found.get(i);
                    user.sendMessage(Text.translatable("message.myfirstmod.survey_find",world.getBlockState(p).getBlock().getName(),p.getX(),p.getY(),p.getZ()),false);
                }
                user.getItemCooldownManager().set(this,100);
            }
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
