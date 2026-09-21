package dev.qynl.myfirstmod.network;

import dev.qynl.myfirstmod.battle.BattleSandbox;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative network layer with strict permissions, rate-limiting, and payload sanitization.
 */
public final class ModPackets {
    private static final Map<UUID, Long> LAST_PACKET_TIME = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BATTLE_TIME = new ConcurrentHashMap<>();
    private static final Set<String> ALLOWED_FORMATIONS = Set.of("line", "flank", "ambush");

    private ModPackets() {}

    // 1. Update Unit Attributes via Sliders
    public record UpdateUnitAttributesPayload(
            float health,
            float damage,
            float armor,
            float scale,
            float speed,
            float retreat
    ) implements CustomPayload {
        public static final CustomPayload.Id<UpdateUnitAttributesPayload> ID =
                new CustomPayload.Id<>(Identifier.of("myfirstmod", "update_unit_attributes"));

        public static final PacketCodec<RegistryByteBuf, UpdateUnitAttributesPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.FLOAT, UpdateUnitAttributesPayload::health,
                PacketCodecs.FLOAT, UpdateUnitAttributesPayload::damage,
                PacketCodecs.FLOAT, UpdateUnitAttributesPayload::armor,
                PacketCodecs.FLOAT, UpdateUnitAttributesPayload::scale,
                PacketCodecs.FLOAT, UpdateUnitAttributesPayload::speed,
                PacketCodecs.FLOAT, UpdateUnitAttributesPayload::retreat,
                UpdateUnitAttributesPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 2. Spawn Unit / Squad assigned to specific Faction (for Solo Sandbox Battles)
    public record SpawnFactionUnitPayload(
            String factionId,
            int count
    ) implements CustomPayload {
        public static final CustomPayload.Id<SpawnFactionUnitPayload> ID =
                new CustomPayload.Id<>(Identifier.of("myfirstmod", "spawn_faction_unit"));

        public static final PacketCodec<RegistryByteBuf, SpawnFactionUnitPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, SpawnFactionUnitPayload::factionId,
                PacketCodecs.INTEGER, SpawnFactionUnitPayload::count,
                SpawnFactionUnitPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 3. Start Granular Custom Battle with Precise Unit Composition & Counts
    public record StartCustomBattlePayload(
            String factionA,
            String unitA,
            int countA,
            String factionB,
            String unitB,
            int countB,
            String formation,
            float distance
    ) implements CustomPayload {
        public static final CustomPayload.Id<StartCustomBattlePayload> ID =
                new CustomPayload.Id<>(Identifier.of("myfirstmod", "start_custom_battle"));

        public static final PacketCodec<RegistryByteBuf, StartCustomBattlePayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeString(value.factionA());
                    buf.writeString(value.unitA());
                    buf.writeInt(value.countA());
                    buf.writeString(value.factionB());
                    buf.writeString(value.unitB());
                    buf.writeInt(value.countB());
                    buf.writeString(value.formation());
                    buf.writeFloat(value.distance());
                },
                buf -> new StartCustomBattlePayload(
                        buf.readString(),
                        buf.readString(),
                        buf.readInt(),
                        buf.readString(),
                        buf.readString(),
                        buf.readInt(),
                        buf.readString(),
                        buf.readFloat()
                )
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(UpdateUnitAttributesPayload.ID, UpdateUnitAttributesPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SpawnFactionUnitPayload.ID, SpawnFactionUnitPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StartCustomBattlePayload.ID, StartCustomBattlePayload.CODEC);

        // 1. Attribute Updates
        ServerPlayNetworking.registerGlobalReceiver(UpdateUnitAttributesPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;

                if (isRateLimited(player.getUuid(), 250)) return; // 250ms limit

                UnitWorldData data = UnitWorldData.get(player.getServer());
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    unit.maxHealth = Math.max(1.0f, Math.min(1000.0f, payload.health()));
                    unit.attackDamage = Math.max(0.0f, Math.min(200.0f, payload.damage()));
                    unit.armor = Math.max(0.0f, Math.min(50.0f, payload.armor()));
                    unit.scale = Math.max(0.2f, Math.min(5.0f, payload.scale()));
                    unit.movementSpeed = Math.max(0.05f, Math.min(1.0f, payload.speed()));
                    unit.retreatHealth = Math.max(0.0f, Math.min(0.9f, payload.retreat()));
                    data.markDirty();
                }
            });
        });

        // 2. Faction Spawns
        ServerPlayNetworking.registerGlobalReceiver(SpawnFactionUnitPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;

                if (isRateLimited(player.getUuid(), 400)) { // 400ms limit
                    player.sendMessage(Text.literal("Please wait a moment before spawning more units.").formatted(Formatting.GRAY), true);
                    return;
                }

                UnitWorldData data = UnitWorldData.get(player.getServer());
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    UnitDefinition soloSpawnUnit = new UnitDefinition(unit.toNbt());

                    String fId = sanitizeId(payload.factionId());
                    if (fId != null && data.factions.containsKey(fId)) {
                        soloSpawnUnit.factionId = fId;
                    }

                    int spawnCount = Math.max(1, Math.min(25, payload.count())); // Clamp to max 25

                    if (spawnCount <= 1) {
                        UnitSpawner.spawnAtPlayer(player, soloSpawnUnit);
                    } else {
                        UnitSpawner.spawnSquadAtPlayer(player, soloSpawnUnit, spawnCount);
                    }
                }
            });
        });

        // 3. Custom Battle Starts
        ServerPlayNetworking.registerGlobalReceiver(StartCustomBattlePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;

                long now = System.currentTimeMillis();
                long lastBattle = LAST_BATTLE_TIME.getOrDefault(player.getUuid(), 0L);
                if (now - lastBattle < 3000) { // 3 second battle cooldown
                    player.sendMessage(Text.literal("Battle deployment in progress. Please wait 3 seconds between battle launches.").formatted(Formatting.YELLOW), true);
                    return;
                }
                LAST_BATTLE_TIME.put(player.getUuid(), now);

                UnitWorldData data = UnitWorldData.get(player.getServer());
                String fA = sanitizeId(payload.factionA());
                String fB = sanitizeId(payload.factionB());
                String uA = sanitizeId(payload.unitA());
                String uB = sanitizeId(payload.unitB());

                if (fA == null || !data.factions.containsKey(fA)) fA = "kingdom";
                if (fB == null || !data.factions.containsKey(fB)) fB = "raiders";
                if (uA == null || uA.isBlank()) uA = "all";
                if (uB == null || uB.isBlank()) uB = "all";

                int cA = Math.max(1, Math.min(50, payload.countA()));
                int cB = Math.max(1, Math.min(50, payload.countB()));

                String form = payload.formation() != null && ALLOWED_FORMATIONS.contains(payload.formation().toLowerCase())
                        ? payload.formation().toLowerCase() : "line";

                float dist = Math.max(12.0f, Math.min(80.0f, payload.distance()));

                BattleSandbox.startCustomBattle(player, fA, uA, cA, fB, uB, cB, form, dist);
            });
        });
    }

    private static boolean isRateLimited(UUID playerId, long minIntervalMs) {
        long now = System.currentTimeMillis();
        long last = LAST_PACKET_TIME.getOrDefault(playerId, 0L);
        if (now - last < minIntervalMs) {
            return true;
        }
        LAST_PACKET_TIME.put(playerId, now);
        return false;
    }

    public static String sanitizeId(String input) {
        if (input == null) return null;
        String clean = input.trim().toLowerCase();
        if (clean.length() > 32) clean = clean.substring(0, 32);
        return clean.matches("^[a-z0-9_]+$") ? clean : null;
    }
}
