package dev.qynl.myfirstmod.kingdom;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.block.SealPlinthBlock;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.realm.RealmFeatures;
import dev.qynl.myfirstmod.realm.RealmState;
import net.minecraft.block.Block;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
/** The royal seals form a chain: attunement is permanent per player, the sanctuary plinth
 * counts the world's chain, and the Cinderwalk Charm answers the vents' heat. */
public final class SealChain {
    public static final int ROOT=1,DROWNED=2,CINDER=4;
    private SealChain(){}
    public static int bit(Item item){
        if(item==ModItems.ROOTBOUND_SEAL)return ROOT;
        if(item==ModItems.DROWNED_SEAL)return DROWNED;
        if(item==ModItems.CINDER_SEAL)return CINDER;
        return 0;}
    public static ActionResult useSeal(ServerPlayerEntity player){
        var stack=player.getMainHandStack();
        int bit=bit(stack.getItem());
        if(bit==0)return ActionResult.PASS;
        var state=RealmState.get(player.getServerWorld());
        var record=state.expedition(player.getUuid());
        if((record.seals&bit)!=0){player.sendMessage(Text.translatable("seal.myfirstmod.known"),true);return ActionResult.SUCCESS;}
        record.seals|=bit;state.worldSeals|=bit;state.markDirty();
        player.sendMessage(Text.translatable("seal.myfirstmod.attuned",stack.getName()),false);
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,SoundCategory.PLAYERS,1,1.1f);
        ensurePlinth(player.getServer());
        return ActionResult.SUCCESS;
    }
    public static void attune(ServerPlayerEntity player,int bit,boolean silent){
        var state=RealmState.get(player.getServerWorld());
        var record=state.expedition(player.getUuid());
        if((record.seals&bit)!=0)return;
        record.seals|=bit;state.worldSeals|=bit;state.markDirty();
        if(!silent)player.sendMessage(Text.translatable("seal.myfirstmod.attuned",Text.translatable("item.myfirstmod.cinder_seal")),false);
        ensurePlinth(player.getServer());
    }
    /** Added only at an empty designated plaza spot, never over player builds. */
    public static void ensurePlinth(MinecraftServer server){
        for(var world:server.getWorlds()){
            var state=RealmState.get(world);
            int count=Integer.bitCount(state.worldSeals);
            if(state.plinthPos!=0){
                var pos=BlockPos.fromLong(state.plinthPos);
                if(world.isChunkLoaded(pos)&&world.getBlockState(pos).isOf(ModBlocks.SEAL_PLINTH)
                        &&world.getBlockState(pos).get(SealPlinthBlock.SEALS)!=count)
                    world.setBlockState(pos,world.getBlockState(pos).with(SealPlinthBlock.SEALS,count),Block.NOTIFY_ALL);
                continue;
            }
            if(!world.getRegistryKey().getValue().getPath().equals("null_realm"))continue;
            for(int[] spot:new int[][]{{5,168},{-5,168},{5,160},{-5,160}}){
                int y=RealmFeatures.surfaceHeight(world,spot[0],spot[1]);
                var pos=new BlockPos(spot[0],y,spot[1]);
                if(!world.getBlockState(pos).isAir()||!world.getBlockState(pos.down()).isSolid())continue;
                world.setBlockState(pos,ModBlocks.SEAL_PLINTH.getDefaultState().with(SealPlinthBlock.SEALS,count),Block.NOTIFY_ALL);
                state.plinthPos=pos.asLong();state.markDirty();
                for(var player:world.getPlayers())player.sendMessage(Text.translatable("seal.myfirstmod.plinth",count),true);
                return;
            }
        }
    }
    public static boolean carries(ServerPlayerEntity player,Item item){
        for(int i=0;i<player.getInventory().size();i++)if(player.getInventory().getStack(i).isOf(item))return true;
        return false;}
    public static boolean charmShields(ServerPlayerEntity player,net.minecraft.entity.damage.DamageSource source){
        return carries(player,ModItems.CINDERWALK_CHARM)&&(source.isOf(DamageTypes.HOT_FLOOR)||source.isOf(DamageTypes.IN_FIRE)||source.isOf(DamageTypes.ON_FIRE));}
    /** Flames die on the skin of a charm bearer. */
    public static void tick(MinecraftServer server){
        if(server.getTicks()%40!=0)return;
        for(var world:server.getWorlds())for(var player:world.getPlayers())
            if(player.isOnFire()&&carries(player,ModItems.CINDERWALK_CHARM))player.setFireTicks(0);
    }
}
