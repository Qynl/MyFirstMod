package dev.qynl.myfirstmod.kingdom;
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
/** Ringing the Tide Bell drains the hall exactly once, exposing the drowned vault and shortcuts. */
public final class ArchiveTides {
    private ArchiveTides(){}
    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos){
        var w=player.getServerWorld();var bell=w.getBlockState(pos);
        if(!bell.isOf(ModBlocks.TIDE_BELL))return ActionResult.PASS;
        if(bell.get(dev.qynl.myfirstmod.block.TideBellBlock.DRAINED)){player.sendMessage(Text.translatable("archive.myfirstmod.once"),true);return ActionResult.SUCCESS;}
        if(!w.canPlayerModifyAt(player,pos))return ActionResult.FAIL;
        for(int dx=-ArchiveRules.radius();dx<=ArchiveRules.radius();dx++)
            for(int dz=-ArchiveRules.radius();dz<=ArchiveRules.radius();dz++)
                if(!w.isChunkLoaded(pos.add(dx,0,dz))){player.sendMessage(Text.translatable("archive.myfirstmod.unloaded"),true);return ActionResult.FAIL;}
        int drained=0;
        for(int dx=-ArchiveRules.radius();dx<=ArchiveRules.radius();dx++)for(int dz=-ArchiveRules.radius();dz<=ArchiveRules.radius();dz++)
            for(int y=ArchiveRules.floorOffset();y<=ArchiveRules.ceilOffset();y++){
                var p=pos.add(dx,y,dz);
                if(w.getBlockState(p).isOf(Blocks.WATER)){w.setBlockState(p,Blocks.AIR.getDefaultState(),Block.NOTIFY_LISTENERS);drained++;}
            }
        w.setBlockState(pos,bell.with(dev.qynl.myfirstmod.block.TideBellBlock.DRAINED,true),Block.NOTIFY_ALL);
        w.playSound(null,pos,SoundEvents.BLOCK_BELL_USE,SoundCategory.BLOCKS,1,.6f);
        w.spawnParticles(ParticleTypes.FALLING_DRIPSTONE_WATER,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,60,3,2,3,.05);
        player.sendMessage(Text.translatable(drained>0?"archive.myfirstmod.drained":"archive.myfirstmod.dry",drained),false);
        return ActionResult.SUCCESS;
    }
}
