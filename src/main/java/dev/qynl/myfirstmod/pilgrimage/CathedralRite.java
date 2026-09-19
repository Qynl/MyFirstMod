package dev.qynl.myfirstmod.pilgrimage;

import dev.qynl.myfirstmod.block.*;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public final class CathedralRite {
    // Offsets from the reliquary at the lowest level, not from the surface.
    public static final int[][] SEALS={{0,16,-4},{0,8,4},{4,0,0}};
    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos) {
        var world=player.getServerWorld();var block=world.getBlockState(pos);
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM) || player.isSpectator()) return ActionResult.PASS;
        if(block.isOf(ModBlocks.MOURNING_RELIQUARY)) {
            if(block.get(FunerarySealBlock.RITE)<3) {
                player.sendMessage(Text.translatable("message.myfirstmod.cathedral_riddle"),false);return ActionResult.SUCCESS;
            }
            // Consume the one-world reward before inventory effects. No reset or repeat reward.
            world.setBlockState(pos,ModBlocks.NULLSTONE_BRICKS.getDefaultState(),Block.NOTIFY_ALL);
            player.getInventory().offerOrDrop(new ItemStack(ModItems.MOURNING_EMBER));
            player.getInventory().offerOrDrop(new ItemStack(ModItems.RESONANT_SHARD,6));
            player.getInventory().offerOrDrop(new ItemStack(ModItems.ECHO_SIGIL));player.addExperience(180);
            var state=RealmState.get(world);state.expedition(player.getUuid()).cathedralsOpened++;state.markDirty();
            world.spawnParticles(ParticleTypes.SOUL,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,45,1,1,1,.02);
            player.sendMessage(Text.translatable("message.myfirstmod.cathedral_reward"),false);return ActionResult.SUCCESS;
        }
        if(!block.isOf(ModBlocks.FUNERARY_SEAL)) return ActionResult.PASS;
        for(int i=0;i<SEALS.length;i++) {
            var root=pos.add(-SEALS[i][0],-SEALS[i][1],-SEALS[i][2]);
            if(!world.isChunkLoaded(root) || !world.getBlockState(root).isOf(ModBlocks.MOURNING_RELIQUARY)) continue;
            var state=world.getBlockState(root);int progress=state.get(FunerarySealBlock.RITE);
            if(progress==3) {player.sendMessage(Text.translatable("message.myfirstmod.cathedral_open"),true);return ActionResult.SUCCESS;}
            int next=PilgrimageRules.advance(progress,i);
            world.setBlockState(root,state.with(FunerarySealBlock.RITE,next),Block.NOTIFY_ALL);
            for(int n=0;n<SEALS.length;n++) {
                var seal=root.add(SEALS[n][0],SEALS[n][1],SEALS[n][2]);
                var old=world.getBlockState(seal);
                if(old.isOf(ModBlocks.FUNERARY_SEAL)) {
                    boolean lit=false;for(int step=0;step<next;step++) if(PilgrimageRules.expectedSeal(step)==n) lit=true;
                    world.setBlockState(seal,old.with(FunerarySealBlock.RITE,lit?3:0),Block.NOTIFY_ALL);
                }
            }
            player.sendMessage(Text.translatable(next==0?"message.myfirstmod.cathedral_wrong":next==3?"message.myfirstmod.cathedral_open":"message.myfirstmod.cathedral_seal",Text.translatable("seal.myfirstmod."+i)),false);
            world.playSound(null,pos,next==0?SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE:SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,SoundCategory.BLOCKS,1,next==0?.5f:.7f+next*.2f);
            return ActionResult.SUCCESS;
        }
        player.sendMessage(Text.translatable("message.myfirstmod.cathedral_spent"),true);return ActionResult.SUCCESS;
    }
}
