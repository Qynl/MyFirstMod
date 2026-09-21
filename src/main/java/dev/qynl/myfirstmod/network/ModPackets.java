package dev.qynl.myfirstmod.network;

import dev.qynl.myfirstmod.battle.BattleSandbox;
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
import net.minecraft.util.Identifier;

public final class ModPackets {
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

        ServerPlayNetworking.registerGlobalReceiver(UpdateUnitAttributesPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;
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

        ServerPlayNetworking.registerGlobalReceiver(SpawnFactionUnitPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;
                UnitWorldData data = UnitWorldData.get(player.getServer());
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    UnitDefinition soloSpawnUnit = new UnitDefinition(unit.toNbt());
                    if (payload.factionId() != null && !payload.factionId().isBlank()) {
                        soloSpawnUnit.factionId = payload.factionId();
                    }

                    if (payload.count() <= 1) {
                        UnitSpawner.spawnAtPlayer(player, soloSpawnUnit);
                    } else {
                        UnitSpawner.spawnSquadAtPlayer(player, soloSpawnUnit, Math.min(25, payload.count()));
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(StartCustomBattlePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;
                BattleSandbox.startCustomBattle(
                        player,
                        payload.factionA(),
                        payload.unitA(),
                        payload.countA(),
                        payload.factionB(),
                        payload.unitB(),
                        payload.countB(),
                        payload.formation(),
                        payload.distance()
                );
            });
        });
    }
}
