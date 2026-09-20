package dev.qynl.myfirstmod.realm;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import java.util.List;
import java.util.Map;

/** Remembrance: opening a landmark cache records the discovery as an advancement.
 *  Progress lives in player data, so saves and shared worlds keep it; the bounty
 *  advancement rewards the full survey once per player. */
public final class LandmarkDiscovery {
    private LandmarkDiscovery(){}
    public static final Map<String,String> CACHE_TO_LANDMARK=Map.of(
        "watch_cache","veil_watchtower",
        "chapel_cache","brine_chapel",
        "geode_cache","geode_garden",
        "camp_cache","slag_camp",
        "caravan_cache","caravan_wreck",
        "fissure_cache","echo_fissure",
        "circle_cache","heartwood_circle",
        "shrine_cache","fen_shrine");
    public static final List<String> LANDMARKS=List.of(
        "veil_watchtower","brine_chapel","geode_garden","slag_camp",
        "caravan_wreck","echo_fissure","heartwood_circle","fen_shrine");
    /** Pure lookup, unit-tested without touching Minecraft classes. */
    public static String landmark(String lootTable){
        if(lootTable==null)return null;
        int slash=lootTable.lastIndexOf('/');
        return CACHE_TO_LANDMARK.get(slash<0?lootTable:lootTable.substring(slash+1));
    }
    public static void register(){
        UseBlockCallback.EVENT.register((player,world,hand,hitResult)->{
            if(world.isClient||!(player instanceof ServerPlayerEntity serverPlayer))return ActionResult.PASS;
            var entity=world.getBlockEntity(hitResult.getBlockPos());
            if(entity==null)return ActionResult.PASS;
            String landmark=landmark(entity.createNbt(world.getRegistryManager()).getString("LootTable"));
            if(landmark==null)return ActionResult.PASS;
            remember(serverPlayer,landmark);
            return ActionResult.PASS;
        });
    }
    private static void remember(ServerPlayerEntity player,String landmark){
        var server=player.getServer();
        var loader=server.getAdvancementLoader();
        var tracker=player.getAdvancementTracker();
        var entry=loader.get(Identifier.of("myfirstmod","landmarks/"+landmark));
        if(entry!=null&&!tracker.getProgress(entry).isDone())tracker.grantCriterion(entry,"discover");
        int found=0;
        for(String kind:LANDMARKS){
            var kindEntry=loader.get(Identifier.of("myfirstmod","landmarks/"+kind));
            if(kindEntry!=null&&tracker.getProgress(kindEntry).isDone())found++;
        }
        if(found==LANDMARKS.size()){
            var crown=loader.get(Identifier.of("myfirstmod","landmarks/cartographer_of_the_null"));
            if(crown!=null&&!tracker.getProgress(crown).isDone())tracker.grantCriterion(crown,"complete");
        }
    }
}
