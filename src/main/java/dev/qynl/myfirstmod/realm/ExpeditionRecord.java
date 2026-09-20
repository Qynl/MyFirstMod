package dev.qynl.myfirstmod.realm;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import java.util.LinkedHashSet;
import java.util.Set;

/** Per-player expedition progress. Bounded discovery storage keeps saves small. */
public final class ExpeditionRecord {
    public int keepClears,pendingKeepRewards;
    public int vow,contractsClaimed,ledgerOffer;
    public long vowReadyAt;
    public final Set<Long> memories=new LinkedHashSet<>();
    public final java.util.Map<Long,String> landmarks=new java.util.LinkedHashMap<>();
    public void rememberLandmark(long pos,String kind) {
        if(!java.util.Set.of("waystone","court","rift","cathedral","memory").contains(kind)) return;
        if(landmarks.size()>=128 && !landmarks.containsKey(pos)) landmarks.remove(landmarks.keySet().iterator().next());
        landmarks.put(pos,kind);
    }
    public int flaskCharges=3,flaskUpgrades,cathedralsOpened;
    public boolean pilgrimKit;
    public int trials, highestTier, victories, pendingNormal, pendingEcho;
    public int riftsClosed,pendingRiftCores;
    public long vigorReadyAt,galeReadyAt;
    public boolean hasWaystone;
    public long boundWaystone;
    public final Set<String> biomes = new LinkedHashSet<>();
    public int seals;
    public final Set<Long> courts = new LinkedHashSet<>();
    public void discoverCourt(long pos) {
        if (courts.size() >= 128 && !courts.contains(pos)) courts.remove(courts.iterator().next());
        courts.add(pos);
    }
    public static ExpeditionRecord read(NbtCompound nbt) {
        ExpeditionRecord record = new ExpeditionRecord();
        record.keepClears=Math.max(0,nbt.getInt("KeepClears"));record.pendingKeepRewards=Math.max(0,Math.min(64,nbt.getInt("PendingKeepRewards")));
        record.vow=dev.qynl.myfirstmod.remembrance.RemembranceRules.vow(nbt.getInt("Vow"));
        record.vowReadyAt=Math.max(0,nbt.getLong("VowReadyAt"));
        record.contractsClaimed=nbt.getInt("ContractsClaimed")&127;record.ledgerOffer=Math.floorMod(nbt.getInt("LedgerOffer"),4);
        long[] memories=nbt.getLongArray("Memories");for(int i=0;i<Math.min(256,memories.length);i++) record.memories.add(memories[i]);
        var landmarks=nbt.getList("Landmarks",10);
        for(int i=Math.max(0,landmarks.size()-128);i<landmarks.size();i++) {
            var entry=landmarks.getCompound(i);record.rememberLandmark(entry.getLong("Pos"),entry.getString("Kind"));
        }
        record.pilgrimKit=nbt.getBoolean("PilgrimKit");
        record.flaskUpgrades=Math.max(0,Math.min(2,nbt.getInt("FlaskUpgrades")));
        record.flaskCharges=dev.qynl.myfirstmod.pilgrimage.PilgrimageRules.charges(nbt.contains("FlaskCharges")?nbt.getInt("FlaskCharges"):3,record.flaskUpgrades);
        record.cathedralsOpened=Math.max(0,nbt.getInt("CathedralsOpened"));
        record.hasWaystone=nbt.getBoolean("HasWaystone");
        record.boundWaystone=nbt.getLong("BoundWaystone");
        record.seals=nbt.getInt("Seals")&15;
        record.riftsClosed=Math.max(0,nbt.getInt("RiftsClosed"));
        record.pendingRiftCores=Math.max(0,Math.min(64,nbt.getInt("PendingRiftCores")));
        record.vigorReadyAt=nbt.getLong("VigorReadyAt");record.galeReadyAt=nbt.getLong("GaleReadyAt");
        record.trials = Math.max(0, nbt.getInt("Trials"));
        record.highestTier = Math.max(0, Math.min(5, nbt.getInt("Tier")));
        record.victories = Math.max(0, nbt.getInt("Victories"));
        record.pendingNormal = Math.max(0,Math.min(64,nbt.getInt("PendingNormal")));
        record.pendingEcho = Math.max(0,Math.min(64,nbt.getInt("PendingEcho")));
        NbtList biomes = nbt.getList("Biomes", 8);
        for (int i=0; i<Math.min(8, biomes.size()); i++) record.biomes.add(biomes.getString(i));
        long[] courts = nbt.getLongArray("Courts");
        for (int i=Math.max(0,courts.length-128); i<courts.length; i++) record.discoverCourt(courts[i]);
        return record;
    }
    public NbtCompound write() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("KeepClears",keepClears);nbt.putInt("PendingKeepRewards",pendingKeepRewards);
        nbt.putInt("Vow",vow);nbt.putLong("VowReadyAt",vowReadyAt);nbt.putInt("ContractsClaimed",contractsClaimed);nbt.putInt("LedgerOffer",ledgerOffer);
        nbt.putLongArray("Memories",memories.stream().mapToLong(Long::longValue).toArray());
        var landmarksNbt=new NbtList();landmarks.forEach((pos,kind)->{var entry=new NbtCompound();entry.putLong("Pos",pos);entry.putString("Kind",kind);landmarksNbt.add(entry);});
        nbt.put("Landmarks",landmarksNbt);
        nbt.putBoolean("PilgrimKit",pilgrimKit);nbt.putInt("FlaskCharges",flaskCharges);nbt.putInt("FlaskUpgrades",flaskUpgrades);
        nbt.putInt("CathedralsOpened",cathedralsOpened);
        nbt.putInt("RiftsClosed",riftsClosed);nbt.putInt("PendingRiftCores",pendingRiftCores);
        nbt.putLong("VigorReadyAt",vigorReadyAt);nbt.putLong("GaleReadyAt",galeReadyAt);
        nbt.putBoolean("HasWaystone",hasWaystone);nbt.putInt("Seals",seals);nbt.putLong("BoundWaystone",boundWaystone);
        nbt.putInt("PendingNormal",pendingNormal);nbt.putInt("PendingEcho",pendingEcho);
        nbt.putInt("Trials", trials); nbt.putInt("Tier", highestTier); nbt.putInt("Victories", victories);
        NbtList list = new NbtList();
        biomes.forEach(b -> list.add(NbtString.of(b)));
        nbt.put("Biomes", list);
        nbt.putLongArray("Courts", courts.stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }
}
