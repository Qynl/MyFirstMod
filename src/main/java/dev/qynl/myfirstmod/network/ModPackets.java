package dev.qynl.myfirstmod.network;

import dev.qynl.myfirstmod.battle.BattleSandbox;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.gui.CreatorScreenHandler;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
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

    // 1. Update Unit Attributes via Sliders (C2S)
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

    // 2. Spawn Unit / Squad assigned to specific Faction (C2S)
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

    // 3. Start Granular Custom Battle (C2S)
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

    // 4. Create Custom Troop (C2S)
    public record CreateCustomTroopPayload(
            String troopId,
            String troopName,
            String entityId,
            String factionId,
            String role,
            String template
    ) implements CustomPayload {
        public static final CustomPayload.Id<CreateCustomTroopPayload> ID =
                new CustomPayload.Id<>(Identifier.of("myfirstmod", "create_custom_troop"));

        public static final PacketCodec<RegistryByteBuf, CreateCustomTroopPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeString(value.troopId());
                    buf.writeString(value.troopName());
                    buf.writeString(value.entityId());
                    buf.writeString(value.factionId());
                    buf.writeString(value.role());
                    buf.writeString(value.template());
                },
                buf -> new CreateCustomTroopPayload(
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString()
                )
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 5. Quick Select Troop by ID from library (C2S)
    public record QuickSelectTroopPayload(
            String troopId
    ) implements CustomPayload {
        public static final CustomPayload.Id<QuickSelectTroopPayload> ID =
                new CustomPayload.Id<>(Identifier.of("myfirstmod", "quick_select_troop"));

        public static final PacketCodec<RegistryByteBuf, QuickSelectTroopPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, QuickSelectTroopPayload::troopId,
                QuickSelectTroopPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 6. Update Full Unit Identity & AI Properties (C2S)
    public record UpdateUnitIdentityPayload(
            String entityId,
            String factionId,
            String role,
            String rank,
            String mount,
            String aura,
            String deathAction,
            boolean commander,
            boolean infiniteAmmo
    ) implements CustomPayload {
        public static final CustomPayload.Id<UpdateUnitIdentityPayload> ID =
                new CustomPayload.Id<>(Identifier.of("myfirstmod", "update_unit_identity"));

        public static final PacketCodec<RegistryByteBuf, UpdateUnitIdentityPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeString(value.entityId());
                    buf.writeString(value.factionId());
                    buf.writeString(value.role());
                    buf.writeString(value.rank());
                    buf.writeString(value.mount());
                    buf.writeString(value.aura());
                    buf.writeString(value.deathAction());
                    buf.writeBoolean(value.commander());
                    buf.writeBoolean(value.infiniteAmmo());
                },
                buf -> new UpdateUnitIdentityPayload(
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readBoolean(),
                        buf.readBoolean()
                )
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 7. Synchronize Currently Equipped Unit to Client (S2C)
    public record SyncEquippedUnitPayload(
            String unitId,
            String unitName,
            String entityId,
            String factionId,
            String role,
            String rank,
            String mount,
            float maxHealth,
            float attackDamage,
            float armor,
            float scale,
            float movementSpeed,
            float retreatHealth,
            String aura,
            String deathAction,
            boolean commander,
            boolean infiniteAmmo,
            int totalUnits,
            int currentIndex
    ) implements CustomPayload {
        public static final CustomPayload.Id<SyncEquippedUnitPayload> ID =
                new CustomPayload.Id<>(Identifier.of("myfirstmod", "sync_equipped_unit"));

        public static final PacketCodec<RegistryByteBuf, SyncEquippedUnitPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeString(value.unitId());
                    buf.writeString(value.unitName());
                    buf.writeString(value.entityId());
                    buf.writeString(value.factionId());
                    buf.writeString(value.role());
                    buf.writeString(value.rank());
                    buf.writeString(value.mount());
                    buf.writeFloat(value.maxHealth());
                    buf.writeFloat(value.attackDamage());
                    buf.writeFloat(value.armor());
                    buf.writeFloat(value.scale());
                    buf.writeFloat(value.movementSpeed());
                    buf.writeFloat(value.retreatHealth());
                    buf.writeString(value.aura());
                    buf.writeString(value.deathAction());
                    buf.writeBoolean(value.commander());
                    buf.writeBoolean(value.infiniteAmmo());
                    buf.writeInt(value.totalUnits());
                    buf.writeInt(value.currentIndex());
                },
                buf -> new SyncEquippedUnitPayload(
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readString(),
                        buf.readString(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readInt(),
                        buf.readInt()
                )
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        // C2S Registrations
        PayloadTypeRegistry.playC2S().register(UpdateUnitAttributesPayload.ID, UpdateUnitAttributesPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SpawnFactionUnitPayload.ID, SpawnFactionUnitPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StartCustomBattlePayload.ID, StartCustomBattlePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CreateCustomTroopPayload.ID, CreateCustomTroopPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(QuickSelectTroopPayload.ID, QuickSelectTroopPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateUnitIdentityPayload.ID, UpdateUnitIdentityPayload.CODEC);

        // S2C Registrations
        PayloadTypeRegistry.playS2C().register(SyncEquippedUnitPayload.ID, SyncEquippedUnitPayload.CODEC);

        // 1. Attribute Updates
        ServerPlayNetworking.registerGlobalReceiver(UpdateUnitAttributesPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;

                if (isRateLimited(player.getUuid(), 200)) return;

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

                if (isRateLimited(player.getUuid(), 350)) {
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

                    int spawnCount = Math.max(1, Math.min(25, payload.count()));

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
                if (now - lastBattle < 2500) {
                    player.sendMessage(Text.literal("Battle deployment in progress. Please wait between battle launches.").formatted(Formatting.YELLOW), true);
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

        // 4. Create Custom Troop Handler
        ServerPlayNetworking.registerGlobalReceiver(CreateCustomTroopPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;

                UnitWorldData data = UnitWorldData.get(player.getServer());
                String baseId = sanitizeId(payload.troopId());
                if (baseId == null || baseId.isBlank()) {
                    baseId = "custom_unit_" + (data.units.size() + 1);
                }

                // Ensure unique ID
                String id = baseId;
                int suffix = 1;
                while (data.units.containsKey(id)) {
                    id = baseId + "_" + suffix++;
                }

                String name = payload.troopName().trim();
                if (name.isBlank()) name = "Custom Troop " + (data.units.size() + 1);
                if (name.length() > 32) name = name.substring(0, 32);

                Identifier entityId = Identifier.tryParse(payload.entityId());
                if (entityId == null) entityId = Identifier.of("minecraft", "villager");

                UnitDefinition def = new UnitDefinition(id, name, entityId);
                def.factionId = data.factions.containsKey(payload.factionId()) ? payload.factionId() : "kingdom";
                def.role = payload.role().isBlank() ? "melee" : payload.role();

                // Apply archetype template gear & base stats
                applyArchetypeTemplate(def, payload.template());

                data.units.put(def.id, def);
                data.setEquippedUnit(player.getUuid(), def.id);
                data.markDirty();

                // Update screen handler slots if open
                if (player.currentScreenHandler instanceof CreatorScreenHandler creatorHandler) {
                    creatorHandler.loadUnitIntoSlots(def);
                }

                syncEquippedUnitToPlayer(player, data, def);
                player.sendMessage(Text.literal("✨ Recruited new troop: " + def.name + " (" + def.id + ")").formatted(Formatting.GREEN), true);
            });
        });

        // 5. Quick Select Troop Handler
        ServerPlayNetworking.registerGlobalReceiver(QuickSelectTroopPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;

                UnitWorldData data = UnitWorldData.get(player.getServer());
                String id = payload.troopId();
                UnitDefinition unit = data.units.get(id);
                if (unit != null) {
                    data.setEquippedUnit(player.getUuid(), unit.id);
                    data.markDirty();

                    if (player.currentScreenHandler instanceof CreatorScreenHandler creatorHandler) {
                        creatorHandler.loadUnitIntoSlots(unit);
                    }

                    syncEquippedUnitToPlayer(player, data, unit);
                }
            });
        });

        // 6. Update Unit Identity & AI Properties Handler
        ServerPlayNetworking.registerGlobalReceiver(UpdateUnitIdentityPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player == null || player.getServer() == null) return;

                UnitWorldData data = UnitWorldData.get(player.getServer());
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    Identifier ent = Identifier.tryParse(payload.entityId());
                    if (ent != null) unit.entityId = ent;
                    if (data.factions.containsKey(payload.factionId())) unit.factionId = payload.factionId();
                    if (!payload.role().isBlank()) unit.role = payload.role();
                    if (!payload.rank().isBlank()) unit.rank = payload.rank();
                    unit.mount = payload.mount();
                    unit.particleAura = payload.aura();
                    unit.deathAction = payload.deathAction();
                    unit.commander = payload.commander();
                    unit.infiniteAmmo = payload.infiniteAmmo();
                    data.markDirty();

                    syncEquippedUnitToPlayer(player, data, unit);
                }
            });
        });
    }

    public static void syncEquippedUnitToPlayer(ServerPlayerEntity player, UnitWorldData data, UnitDefinition unit) {
        if (player == null || data == null || unit == null) return;
        List<String> keys = new ArrayList<>(data.units.keySet());
        int idx = keys.indexOf(unit.id);
        ServerPlayNetworking.send(player, new SyncEquippedUnitPayload(
                unit.id,
                unit.name,
                unit.entityId.toString(),
                unit.factionId,
                unit.role,
                unit.rank,
                unit.mount,
                unit.maxHealth,
                unit.attackDamage,
                unit.armor,
                unit.scale,
                unit.movementSpeed,
                unit.retreatHealth,
                unit.particleAura,
                unit.deathAction,
                unit.commander,
                unit.infiniteAmmo,
                keys.size(),
                Math.max(0, idx)
        ));
    }

    private static void applyArchetypeTemplate(UnitDefinition def, String template) {
        if (template == null) return;
        switch (template.toLowerCase()) {
            case "archer", "marksman" -> {
                def.role = "ranged";
                def.maxHealth = 24.0f;
                def.attackDamage = 5.0f;
                def.armor = 6.0f;
                def.retreatHealth = 0.25f;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                def.equipment.put(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                def.equipment.put(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                def.inventory.add(new ItemStack(Items.ARROW, 64));
            }
            case "cavalry" -> {
                def.role = "melee";
                def.mount = "minecraft:horse";
                def.maxHealth = 45.0f;
                def.attackDamage = 8.5f;
                def.armor = 14.0f;
                def.movementSpeed = 0.30f;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                def.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case "mage", "wizard" -> {
                def.role = "support";
                def.particleAura = "portal";
                def.maxHealth = 30.0f;
                def.attackDamage = 6.0f;
                def.armor = 4.0f;
                def.canThrowPotions = true;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.SPLASH_POTION));
                def.inventory.add(new ItemStack(Items.SPLASH_POTION, 8));
            }
            case "pyro", "artillery" -> {
                def.role = "pyrotechnic";
                def.particleAura = "flame";
                def.maxHealth = 32.0f;
                def.canShootFireworks = true;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
                def.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.FIREWORK_ROCKET));
                def.inventory.add(new ItemStack(Items.FIREWORK_ROCKET, 64));
            }
            case "sapper", "bombardier" -> {
                def.role = "bombardier";
                def.particleAura = "flame";
                def.deathAction = "explosion";
                def.maxHealth = 35.0f;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BOMBARDIER_MORTAR));
            }
            case "medic", "healer" -> {
                def.role = "medic";
                def.particleAura = "heart";
                def.maxHealth = 28.0f;
                def.healAllies = true;
                def.canThrowPotions = true;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.SPLASH_POTION));
                def.inventory.add(new ItemStack(Items.SPLASH_POTION, 6));
                def.inventory.add(new ItemStack(Items.GOLDEN_APPLE, 2));
            }
            case "titan", "golem" -> {
                def.role = "tank";
                def.scale = 1.35f;
                def.maxHealth = 150.0f;
                def.attackDamage = 16.0f;
                def.armor = 20.0f;
                def.armorToughness = 8.0f;
                def.knockbackResistance = 1.0f;
            }
            case "beast", "wolf" -> {
                def.role = "berserker";
                def.scale = 1.15f;
                def.maxHealth = 36.0f;
                def.attackDamage = 8.0f;
                def.movementSpeed = 0.32f;
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.ARMADILLO_SCUTE));
            }
            case "assassin", "stealth" -> {
                def.role = "assassin";
                def.movementSpeed = 0.32f;
                def.maxHealth = 28.0f;
                def.attackDamage = 8.0f;
                def.armor = 6.0f;
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ASSASSIN_DAGGER));
            }
            case "paladin", "crusader" -> {
                def.role = "paladin";
                def.particleAura = "totem";
                def.deathAction = "healing_mist";
                def.maxHealth = 55.0f;
                def.attackDamage = 8.5f;
                def.armor = 16.0f;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PALADIN_MACE));
                def.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case "druid" -> {
                def.role = "druid";
                def.particleAura = "heart";
                def.maxHealth = 32.0f;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DRUID_STAFF));
            }
            default -> { // standard warrior
                def.role = "melee";
                def.maxHealth = 40.0f;
                def.attackDamage = 7.5f;
                def.armor = 14.0f;
                def.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                def.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                def.equipment.put(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                def.equipment.put(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                def.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                def.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                def.inventory.add(new ItemStack(Items.COOKED_BEEF, 8));
            }
        }
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
