package dev.qynl.myfirstmod.kingdom;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import net.minecraft.block.Block;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.*;
import java.util.*;
public final class Monastery {
    private static final Map<Long,Integer> MISSING=new HashMap<>();
    public static BlockPos relative(BlockPos root,Direction facing,int right,int up,int forward){
        var side=facing.rotateYClockwise();return root.add(side.getOffsetX()*right+facing.getOffsetX()*forward,up,side.getOffsetZ()*right+facing.getOffsetZ()*forward);
    }
    public static ActionResult interact(ServerPlayerEntity p,BlockPos pos){
        var w=p.getServerWorld();var block=w.getBlockState(pos);
        if(p.isSpectator()||!w.getRegistryKey().equals(VoidPortalManager.NULL_REALM))return ActionResult.PASS;
        if(!block.isOf(ModBlocks.ROOT_HEART)&&!block.isOf(ModBlocks.CLOISTER_BELL)&&!block.isOf(ModBlocks.ROOT_RELIQUARY))return ActionResult.PASS;
        if(!w.canPlayerModifyAt(p,pos))return ActionResult.FAIL;
        if(block.isOf(ModBlocks.ROOT_RELIQUARY)){
            w.setBlockState(pos,ModBlocks.NULLSTONE_BRICKS.getDefaultState(),Block.NOTIFY_ALL);
            p.getInventory().offerOrDrop(new ItemStack(ModItems.ROOTBOUND_SEAL));
            p.getInventory().offerOrDrop(new ItemStack(ModItems.RESONITE_INGOT,3));
            p.getInventory().offerOrDrop(new ItemStack(ModItems.DUSK_FIBER));p.addExperience(250);
            p.sendMessage(Text.translatable("monastery.myfirstmod.reward"),false);return ActionResult.SUCCESS;
        }
        if(block.isOf(ModBlocks.CLOISTER_BELL)){
            var facing=block.get(MonasteryBlock.FACING);
            for(int side=0;side<2;side++){
                var root=relative(pos,facing,side==0?16:-16,side==0?0:-6,16);
                if(!w.isChunkLoaded(root))continue;
                var heart=w.getBlockState(root);
                if(!heart.isOf(ModBlocks.ROOT_HEART)||heart.get(MonasteryBlock.FACING)!=facing)continue;
                if(!w.canPlayerModifyAt(p,root))return ActionResult.FAIL;
                w.setBlockState(root,heart.with(MonasteryBlock.BELLS,MonasteryRules.ringBell(heart.get(MonasteryBlock.BELLS),side)),Block.NOTIFY_ALL);
                w.setBlockState(pos,block.with(MonasteryBlock.BELLS,3),Block.NOTIFY_ALL);
                w.playSound(null,pos,net.minecraft.sound.SoundEvents.BLOCK_BELL_USE,net.minecraft.sound.SoundCategory.BLOCKS,1,.7f+side*.3f);
                p.sendMessage(Text.translatable("monastery.myfirstmod.bell"),false);return ActionResult.SUCCESS;
            }
            p.sendMessage(Text.translatable("monastery.myfirstmod.spent"),true);return ActionResult.SUCCESS;
        }
        if(block.get(MonasteryBlock.BELLS)!=3){p.sendMessage(Text.translatable("monastery.myfirstmod.riddle"),false);return ActionResult.SUCCESS;}
        if(w.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){p.sendMessage(Text.translatable("monastery.myfirstmod.peaceful"),true);return ActionResult.SUCCESS;}
        var data=RealmState.get(w);
        if(data.monasteryGuardians.containsKey(pos.asLong())){p.sendMessage(Text.translatable("monastery.myfirstmod.active"),true);return ActionResult.SUCCESS;}
        // The actor is altar-bound: it stays in exactly the heart's chunk, including after restart.
        var boss=ModEntities.ROOTBOUND_PRIOR.create(w);if(boss==null)return ActionResult.FAIL;
        boss.refreshPositionAndAngles(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        if(!w.isSpaceEmpty(boss)){boss.discard();p.sendMessage(Text.translatable("monastery.myfirstmod.blocked"),true);return ActionResult.FAIL;}
        int count=(int)w.getPlayers().stream().filter(q->q.isAlive()&&!q.isCreative()&&!q.isSpectator()&&q.squaredDistanceTo(boss)<24*24).count();
        boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(MonasteryRules.health(count));boss.setHealth(boss.getMaxHealth());boss.bind(pos);boss.setPersistent();
        if(!w.spawnEntity(boss))return ActionResult.FAIL;
        data.monasteryGuardians.put(pos.asLong(),boss.getUuid());data.markDirty();
        p.sendMessage(Text.translatable("monastery.myfirstmod.begin"),false);return ActionResult.SUCCESS;
    }
    public static void tick(MinecraftServer server){
        var w=server.getWorld(VoidPortalManager.NULL_REALM);if(w==null||w.getTime()%20!=0)return;
        var data=RealmState.get(w);
        for(var e:new ArrayList<>(data.monasteryGuardians.entrySet())){
            var root=BlockPos.fromLong(e.getKey());
            if(!w.isChunkLoaded(root)||w.getPlayers().stream().noneMatch(p->p.squaredDistanceTo(Vec3d.ofCenter(root))<48*48)){MISSING.remove(e.getKey());continue;}
            if(w.getEntity(e.getValue()) instanceof RootboundPriorEntity){MISSING.remove(e.getKey());continue;}
            // Six seconds for asynchronous entity loading. Removed/Peaceful actors do not award loot.
            if(MISSING.merge(e.getKey(),1,Integer::sum)>=6){data.monasteryGuardians.remove(e.getKey());MISSING.remove(e.getKey());data.markDirty();}
        }
    }
    public static void clear(){MISSING.clear();}
}
