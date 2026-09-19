package dev.qynl.myfirstmod.boss;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NullWardenState extends PersistentState {
    public static final String ID = "myfirstmod_null_warden";

    public UUID bossUuid;
    public final Set<UUID> participants = new HashSet<>();
    public final Set<UUID> eligiblePlayers = new HashSet<>();
    public final Set<UUID> rewardedPlayers = new HashSet<>();
    public final Set<UUID> echoes = new HashSet<>();
    public final Map<UUID, ReturnPointData> returnPoints = new HashMap<>();
    public boolean defeated;
    public boolean everDefeated, rematch, rewardsQueued;
    public int challengeTier;
    public boolean rewarded;
    public boolean returnPortalBuilt;
    public int phase = 1;
    public int activePylons;
    public int cleansedPylons;
    public int[] pylonProgress = new int[4];

    public static Type<NullWardenState> type() {
        return new Type<>(
                NullWardenState::new,
                (nbt, lookup) -> fromNbt(nbt),
                DataFixTypes.SAVED_DATA_RAIDS
        );
    }

    private static NullWardenState fromNbt(NbtCompound nbt) {
        NullWardenState state = new NullWardenState();

        if (nbt.contains("BossUuid")) {
            try {
                state.bossUuid = UUID.fromString(nbt.getString("BossUuid"));
            } catch (IllegalArgumentException ignored) {
            }
        }

        state.defeated = nbt.getBoolean("Defeated");
        state.everDefeated = nbt.getBoolean("EverDefeated") || state.defeated;
        state.rematch = nbt.getBoolean("Rematch");
        state.rewardsQueued = nbt.getBoolean("RewardsQueued");
        state.challengeTier = Math.max(0,Math.min(3,nbt.getInt("ChallengeTier")));
        state.rewarded = nbt.getBoolean("Rewarded");
        state.returnPortalBuilt = nbt.getBoolean("ReturnPortalBuilt");
        state.phase = Math.max(1, nbt.getInt("Phase"));
        state.activePylons = nbt.getInt("ActivePylons");
        state.cleansedPylons = nbt.getInt("CleansedPylons") & 0xF;
        if (nbt.contains("PylonProgress", 9)) {
            NbtList progress = nbt.getList("PylonProgress", 3);
            for (int i = 0; i < Math.min(4, progress.size()); i++) {
                state.pylonProgress[i] = Math.max(0, Math.min(50, progress.getInt(i)));
            }
        }

        readUuidList(nbt, "Participants", state.participants);
        readUuidList(nbt, "EligiblePlayers", state.eligiblePlayers);
        readUuidList(nbt, "RewardedPlayers", state.rewardedPlayers);
        readUuidList(nbt, "Echoes", state.echoes);

        NbtList returns = nbt.getList("ReturnPoints", 10);
        for (int i = 0; i < returns.size(); i++) {
            NbtCompound entry = returns.getCompound(i);
            try {
                UUID uuid = UUID.fromString(entry.getString("Player"));
                state.returnPoints.put(uuid, new ReturnPointData(
                        entry.getString("World"),
                        entry.getInt("X"),
                        entry.getInt("Y"),
                        entry.getInt("Z"),
                        entry.getFloat("Yaw"),
                        entry.getFloat("Pitch")
                ));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return state;
    }

    private static void readUuidList(NbtCompound nbt, String key, Set<UUID> target) {
        NbtList list = nbt.getList(key, 8);
        for (int i = 0; i < list.size(); i++) {
            try {
                target.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void writeUuidList(NbtCompound nbt, String key, Set<UUID> source) {
        NbtList list = new NbtList();
        for (UUID uuid : source) list.add(net.minecraft.nbt.NbtString.of(uuid.toString()));
        nbt.put(key, list);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (bossUuid != null) nbt.putString("BossUuid", bossUuid.toString());

        nbt.putBoolean("Defeated", defeated);
        nbt.putBoolean("EverDefeated", everDefeated);
        nbt.putBoolean("Rematch", rematch);
        nbt.putBoolean("RewardsQueued",rewardsQueued);
        nbt.putInt("ChallengeTier", challengeTier);
        nbt.putBoolean("Rewarded", rewarded);
        nbt.putBoolean("ReturnPortalBuilt", returnPortalBuilt);
        nbt.putInt("Phase", phase);
        nbt.putInt("ActivePylons", activePylons);
        nbt.putInt("CleansedPylons", cleansedPylons);
        NbtList progress = new NbtList();
        for (int value : pylonProgress) progress.add(net.minecraft.nbt.NbtInt.of(Math.max(0, Math.min(50, value))));
        nbt.put("PylonProgress", progress);

        writeUuidList(nbt, "Participants", participants);
        writeUuidList(nbt, "EligiblePlayers", eligiblePlayers);
        writeUuidList(nbt, "RewardedPlayers", rewardedPlayers);
        writeUuidList(nbt, "Echoes", echoes);

        NbtList returns = new NbtList();
        for (Map.Entry<UUID, ReturnPointData> entry : returnPoints.entrySet()) {
            ReturnPointData point = entry.getValue();
            NbtCompound data = new NbtCompound();
            data.putString("Player", entry.getKey().toString());
            data.putString("World", point.worldId);
            data.putInt("X", point.x);
            data.putInt("Y", point.y);
            data.putInt("Z", point.z);
            data.putFloat("Yaw", point.yaw);
            data.putFloat("Pitch", point.pitch);
            returns.add(data);
        }
        nbt.put("ReturnPoints", returns);

        return nbt;
    }

    public void dirty() {
        markDirty();
    }

    public record ReturnPointData(String worldId, int x, int y, int z, float yaw, float pitch) {
    }
}