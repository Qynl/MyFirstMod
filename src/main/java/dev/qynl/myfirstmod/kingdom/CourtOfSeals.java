package dev.qynl.myfirstmod.kingdom;
import dev.qynl.myfirstmod.block.CrownGateBlock;
import dev.qynl.myfirstmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.registry.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
/** The Silent Court convenes in three sessions when a thrice-attuned pilgrim opens the
 * Crown Gate. Outlive them and the throne treasury opens once for the whole world. */
public final class CourtOfSeals {
    private CourtOfSeals(){}
    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos){
        var w=player.getServerWorld();var gate=w.getBlockState(pos);
        if(!gate.isOf(ModBlocks.CROWN_GATE))return ActionResult.PASS;
        int phase=gate.get(CrownGateBlock.PHASE);
        if(phase==2){player.sendMessage(Text.translatable("court.myfirstmod.ruled"),true);return ActionResult.SUCCESS;}
        if(phase==1){player.sendMessage(Text.translatable("court.myfirstmod.session"),true);return ActionResult.SUCCESS;}
        var state=dev.qynl.myfirstmod.realm.RealmState.get(w);
        var record=state.expedition(player.getUuid());
        if((record.seals&7)!=7){player.sendMessage(Text.translatable("court.myfirstmod.unworthy"),true);return ActionResult.SUCCESS;}
        if(!w.canPlayerModifyAt(player,pos))return ActionResult.FAIL;
        w.setBlockState(pos,gate.with(CrownGateBlock.PHASE,1),Block.NOTIFY_ALL);
        state.courtPos=pos.asLong();state.courtWave=1;state.courtActors.clear();state.markDirty();
        spawn(w,pos,0);
        player.sendMessage(Text.translatable("court.myfirstmod.open"),false);
        return ActionResult.SUCCESS;
    }
    private static void spawn(ServerWorld w,BlockPos gate,int session){
        var sessionIds=CourtRules.sessions()[session];
        for(int slot=0;slot<sessionIds.length;slot++){
            var type=Registries.ENTITY_TYPE.get(Identifier.of(sessionIds[slot]));
            if(type==null)continue;
            var off=CourtRules.offsets()[CourtRules.offsetFor(session,slot)];
            var entity=type.create(w);
            if(entity==null)continue;
            entity.refreshPositionAndAngles(gate.getX()+off[0],gate.getY()+off[1],gate.getZ()+off[2],0,0);
            w.spawnEntity(entity);
            var state=dev.qynl.myfirstmod.realm.RealmState.get(w);
            if(state.courtActors.size()<16)state.courtActors.add(entity.getUuid());
        }
        state(w).markDirty();
    }
    private static dev.qynl.myfirstmod.realm.RealmState state(ServerWorld w){return dev.qynl.myfirstmod.realm.RealmState.get(w);}
    public static void tick(MinecraftServer server){
        var w=server.getWorld(dev.qynl.myfirstmod.portal.VoidPortalManager.NULL_REALM);
        if(w==null)return;
        var state=dev.qynl.myfirstmod.realm.RealmState.get(w);
        if(state.courtPos==0||state.courtWave==0)return;
        var gate=BlockPos.fromLong(state.courtPos);
        if(!w.isChunkLoaded(gate))return;
        if(!w.getBlockState(gate).isOf(ModBlocks.CROWN_GATE)||w.getBlockState(gate).get(CrownGateBlock.PHASE)!=1)return;
        state.courtActors.removeIf(id->{var e=w.getEntity(id);return e==null||!e.isAlive();});
        if(!state.courtActors.isEmpty())return;
        if(state.courtWave<CourtRules.waves()){
            int next=state.courtWave;state.courtWave=next+1;state.markDirty();
            spawn(w,gate,next);
            for(var player:w.getPlayers())player.sendMessage(Text.translatable("court.myfirstmod.wave",next+1),true);
        } else {
            w.setBlockState(gate,w.getBlockState(gate).with(CrownGateBlock.PHASE,2),Block.NOTIFY_ALL);
            var chest=gate.add(CourtRules.chestOffset()[0],CourtRules.chestOffset()[1],CourtRules.chestOffset()[2]);
            if(w.getBlockState(chest).isAir()){
                w.setBlockState(chest,Blocks.CHEST.getDefaultState(),Block.NOTIFY_LISTENERS);
                LootableInventory.setLootTable(w,w.getRandom(),chest,RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of("myfirstmod","chests/crown_cache")));
            }
            state.courtWave=0;state.courtActors.clear();state.markDirty();
            for(var player:w.getPlayers())player.sendMessage(Text.translatable("court.myfirstmod.ruled"),false);
        }
    }
}
