package dev.qynl.myfirstmod.kingdom;
import dev.qynl.myfirstmod.block.CrucibleBlock;
import dev.qynl.myfirstmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
/** Quenching the Ember Crucible is one-way and gated on the seal chain: the fire shell
 * falls, the lava channels cool to slagglass, and the vault opens. */
public final class FoundryQuench {
    private FoundryQuench(){}
    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos){
        var w=player.getServerWorld();var crucible=w.getBlockState(pos);
        if(!crucible.isOf(ModBlocks.EMBER_CRUCIBLE))return ActionResult.PASS;
        if(crucible.get(CrucibleBlock.QUENCHED)){player.sendMessage(Text.translatable("foundry.myfirstmod.once"),true);return ActionResult.SUCCESS;}
        var record=dev.qynl.myfirstmod.realm.RealmState.get(w).expedition(player.getUuid());
        if((record.seals&SealChain.ROOT)==0||(record.seals&SealChain.DROWNED)==0){
            player.sendMessage(Text.translatable("foundry.myfirstmod.unworthy"),true);return ActionResult.SUCCESS;}
        if(!w.canPlayerModifyAt(player,pos))return ActionResult.FAIL;
        for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++)
            if(!w.isChunkLoaded(pos.add(dx,0,dz))){player.sendMessage(Text.translatable("foundry.myfirstmod.unloaded"),true);return ActionResult.FAIL;}
        for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++)
            if(FoundryRules.inShell(dx,dz))for(int y=FoundryRules.shellFloorOffset();y<=FoundryRules.shellCeilOffset();y++){
                var p=pos.add(dx,y,dz);
                if(w.getBlockState(p).isOf(Blocks.LAVA))w.setBlockState(p,Blocks.AIR.getDefaultState(),Block.NOTIFY_LISTENERS);
            }
        for(int row:new int[]{-FoundryRules.channelRow(),FoundryRules.channelRow()})
            for(int dx=-FoundryRules.channelHalfLength();dx<=FoundryRules.channelHalfLength();dx++){
                var p=pos.add(dx,FoundryRules.shellFloorOffset(),row);
                if(w.getBlockState(p).isOf(Blocks.LAVA))w.setBlockState(p,ModBlocks.SLAGGLASS.getDefaultState(),Block.NOTIFY_LISTENERS);
            }
        w.setBlockState(pos,crucible.with(CrucibleBlock.QUENCHED,true),Block.NOTIFY_ALL);
        w.playSound(null,pos,SoundEvents.BLOCK_FIRE_EXTINGUISH,SoundCategory.BLOCKS,1,.7f);
        w.spawnParticles(ParticleTypes.WHITE_ASH,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,80,3,2,3,.04);
        player.sendMessage(Text.translatable("foundry.myfirstmod.quenched"),false);
        SealChain.attune(player,SealChain.CINDER,true);
        return ActionResult.SUCCESS;
    }
}
