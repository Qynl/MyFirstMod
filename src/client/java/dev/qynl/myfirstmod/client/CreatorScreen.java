package dev.qynl.myfirstmod.client;

import dev.qynl.myfirstmod.gui.CreatorScreenHandler;
import dev.qynl.myfirstmod.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public final class CreatorScreen extends HandledScreen<CreatorScreenHandler> {
    private int tab = 0;

    // Tactical Military Palette
    private static final int COLOR_BG_BACKDROP = 0xd9060b14;
    private static final int COLOR_PANEL_MAIN = 0xff0f172a;
    private static final int COLOR_PANEL_INNER = 0xff090e1a;
    private static final int COLOR_CARD_BG = 0xff1e293b;
    private static final int COLOR_CARD_BORDER = 0xff475569;
    private static final int COLOR_GOLD_ACCENT = 0xfff59e0b;
    private static final int COLOR_CYAN_ACCENT = 0xff06b6d4;
    private static final int COLOR_GREEN_ACCENT = 0xff10b981;
    private static final int COLOR_RED_ACCENT = 0xffef4444;

    // Synchronized Unit Profile State
    private String currentUnitId = "royal_knight";
    private String currentUnitName = "Royal Knight";
    private String currentEntityId = "minecraft:villager";
    private String currentFactionId = "kingdom";
    private String currentRole = "melee";
    private String currentRank = "captain";
    private String currentMount = "";
    private String currentAura = "none";
    private String currentDeathAction = "none";
    private boolean currentCommander = false;
    private boolean currentInfiniteAmmo = false;
    private int totalUnitCount = 1;
    private int currentUnitIndex = 0;

    // Real-Time Slider Values
    private double sliderHealth = 40.0;
    private double sliderDamage = 7.5;
    private double sliderArmor = 15.0;
    private double sliderScale = 1.0;
    private double sliderSpeed = 0.25;
    private double sliderRetreat = 0.20;

    // --- NEW TROOP STUDIO STATE ---
    private TextFieldWidget newTroopNameField;
    private TextFieldWidget newTroopIdField;
    private int newTroopArchetypeIdx = 0;
    private int newTroopEntityIdx = 0;
    private int newTroopFactionIdx = 0;

    private static final String[] ARCHETYPE_KEYS = {
            "warrior", "archer", "cavalry", "mage", "pyro",
            "sapper", "medic", "titan", "beast", "assassin", "paladin", "druid"
    };
    private static final String[] ARCHETYPE_NAMES = {
            "🛡 Frontline Warrior", "🏹 Long-range Marksman", "🐎 Armored Cavalry", "🧙 Arcane Mage",
            "💥 Fireworks Pyro", "💣 Siege Sapper (TNT)", "🧪 Field Medic", "🗿 Colossal Titan",
            "🐺 Savage Beast", "🗡 Shadow Assassin", "⚡ Holy Paladin", "🌿 Grove Druid"
    };

    private static final String[] ENTITY_CHOICES = {
            "minecraft:villager", "minecraft:skeleton", "minecraft:zombie", "minecraft:pillager",
            "minecraft:vindicator", "minecraft:piglin_brute", "minecraft:piglin", "minecraft:witch",
            "minecraft:evoker", "minecraft:iron_golem", "minecraft:wolf", "minecraft:polar_bear",
            "minecraft:breeze", "minecraft:bogged", "minecraft:allay", "minecraft:blaze",
            "minecraft:wither_skeleton", "minecraft:stray", "minecraft:drowned", "minecraft:warden",
            "minecraft:ravager", "minecraft:camel"
    };
    private static final String[] ENTITY_DISPLAY_NAMES = {
            "Villager", "Skeleton", "Zombie", "Pillager",
            "Vindicator", "Piglin Brute", "Piglin", "Witch",
            "Evoker", "Iron Golem", "Wolf", "Polar Bear",
            "Breeze", "Bogged", "Allay", "Blaze",
            "Wither Skeleton", "Stray", "Drowned", "Warden",
            "Ravager", "Camel"
    };

    // --- LIBRARY STATE ---
    private TextFieldWidget searchField;

    // --- CUSTOM BATTLE CONTROLS ---
    private static final String[] FACTION_OPTIONS = {"kingdom", "raiders", "villagers", "undead", "arcane"};
    private static final String[] FACTION_NAMES = {"🔵 Kingdom", "🔴 Raiders", "🟢 Villagers", "🟣 Undead", "🔷 Arcane"};

    private static final String[] UNIT_OPTIONS = {
            "all", "royal_knight", "royal_cavalry", "war_wolf", "battle_bear",
            "desert_dragoon", "holy_paladin", "iron_titan", "grove_druid",
            "royal_pyro", "siege_bombardier", "raider_berserker", "undead_necromancer",
            "shadow_assassin", "dread_cavalry", "bogged_sniper", "tempest_breeze", "pixie_medic"
    };
    private static final String[] UNIT_NAMES = {
            "All (Mixed Battalion)", "Royal Knight", "Royal Cavalry", "War Wolf", "Battle Bear",
            "Camel Dragoon", "Holy Paladin", "Colossal Iron Titan", "Grove Druid",
            "Fireworks Artillery", "Siege Bombardier", "Raider Berserker", "Undead Necromancer",
            "Shadow Assassin", "Dread Cavalry", "Bogged Sniper", "Tempest Breeze", "Pixie Medic"
    };

    private static final String[] FORMATION_OPTIONS = {"line", "flank", "ambush"};
    private static final String[] FORMATION_NAMES = {"Line vs Line", "Pincer Flank", "Ambush Encircle"};

    private static final float[] DISTANCE_OPTIONS = {18.0f, 32.0f, 50.0f};
    private static final String[] DISTANCE_NAMES = {"Close (18m)", "Standard (32m)", "Long (50m)"};

    private int battleFactionAIdx = 0;
    private int battleUnitAIdx = 0;
    private double battleCountA = 12.0;

    private int battleFactionBIdx = 1;
    private int battleUnitBIdx = 0;
    private double battleCountB = 12.0;

    private int battleFormationIdx = 0;
    private int battleDistanceIdx = 1;

    // Simulation toggle states
    private boolean buildingEnabled = true;
    private boolean potionsEnabled = true;
    private boolean fireworksEnabled = true;
    private boolean shieldEnabled = true;
    private boolean friendlyFireAllowed = false;

    // 3D Dummy Entity Cache
    private final Map<String, LivingEntity> dummyEntityCache = new HashMap<>();

    public CreatorScreen(CreatorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 430;
        this.backgroundHeight = 250;
    }

    @Override
    protected void init() {
        super.init();
        rebuildInterface();
    }

    public void applySyncedUnit(ModPackets.SyncEquippedUnitPayload payload) {
        this.currentUnitId = payload.unitId();
        this.currentUnitName = payload.unitName();
        this.currentEntityId = payload.entityId();
        this.currentFactionId = payload.factionId();
        this.currentRole = payload.role();
        this.currentRank = payload.rank();
        this.currentMount = payload.mount();
        this.currentAura = payload.aura();
        this.currentDeathAction = payload.deathAction();
        this.currentCommander = payload.commander();
        this.currentInfiniteAmmo = payload.infiniteAmmo();
        this.totalUnitCount = payload.totalUnits();
        this.currentUnitIndex = payload.currentIndex();

        this.sliderHealth = payload.maxHealth();
        this.sliderDamage = payload.attackDamage();
        this.sliderArmor = payload.armor();
        this.sliderScale = payload.scale();
        this.sliderSpeed = payload.movementSpeed();
        this.sliderRetreat = payload.retreatHealth();

        if (tab == 0) {
            rebuildInterface();
        }
    }

    private void rebuildInterface() {
        clearChildren();

        int topY = y + 6;
        int tabWidth = 66;

        // Navigation Tab Bar with active highlighted styling
        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 0 ? "⚔ [EDITOR]" : "⚔ Editor"), b -> { tab = 0; rebuildInterface(); })
                .dimensions(x + 8 + (0 * (tabWidth + 3)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 1 ? "✨ [NEW]" : "✨ New"), b -> { tab = 1; rebuildInterface(); })
                .dimensions(x + 8 + (1 * (tabWidth + 3)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 2 ? "📋 [UNITS]" : "📋 Units"), b -> { tab = 2; rebuildInterface(); })
                .dimensions(x + 8 + (2 * (tabWidth + 3)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 3 ? "🛡 [DIPLO]" : "🛡 Diplo"), b -> { tab = 3; rebuildInterface(); })
                .dimensions(x + 8 + (3 * (tabWidth + 3)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 4 ? "💥 [BATTLE]" : "💥 Battle"), b -> { tab = 4; rebuildInterface(); })
                .dimensions(x + 8 + (4 * (tabWidth + 3)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 5 ? "⚙ [CONFIG]" : "⚙ Config"), b -> { tab = 5; rebuildInterface(); })
                .dimensions(x + 8 + (5 * (tabWidth + 3)), topY, tabWidth, 20).build());

        int leftX = x + 12;
        int rightX = x + 248;

        if (tab == 0) { // TAB 0: UNIT EDITOR WITH 3D PREVIEW & STAT SLIDERS
            // Unit Selectors
            addDrawableChild(ButtonWidget.builder(Text.literal("⮜ Prev Unit"), b -> sendButton(6))
                    .dimensions(leftX, y + 30, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Next Unit ⮞"), b -> sendButton(3))
                    .dimensions(leftX + 82, y + 30, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("✨ + New"), b -> { tab = 1; rebuildInterface(); })
                    .dimensions(leftX + 164, y + 30, 56, 16).build());

            // 1. Health Slider (5 to 300 HP)
            addDrawableChild(new TacticalSliderWidget(leftX, y + 48, 108, 16, "Health", "HP", 5.0, 300.0, sliderHealth, val -> {
                sliderHealth = val;
                syncSlidersToServer();
            }));

            // 2. Attack Damage Slider (1 to 50 DMG)
            addDrawableChild(new TacticalSliderWidget(leftX + 112, y + 48, 108, 16, "Damage", "DMG", 1.0, 50.0, sliderDamage, val -> {
                sliderDamage = val;
                syncSlidersToServer();
            }));

            // 3. Armor Slider (0 to 30 Armor)
            addDrawableChild(new TacticalSliderWidget(leftX, y + 66, 108, 16, "Armor", "pts", 0.0, 30.0, sliderArmor, val -> {
                sliderArmor = val;
                syncSlidersToServer();
            }));

            // 4. Visual Scale Slider (0.25x to 3.00x)
            addDrawableChild(new TacticalSliderWidget(leftX + 112, y + 66, 108, 16, "Scale", "x", 0.25, 3.0, sliderScale, val -> {
                sliderScale = val;
                syncSlidersToServer();
            }));

            // 5. Movement Speed Slider (0.10 to 0.60)
            addDrawableChild(new TacticalSliderWidget(leftX, y + 84, 108, 16, "Speed", "spd", 0.10, 0.60, sliderSpeed, val -> {
                sliderSpeed = val;
                syncSlidersToServer();
            }));

            // 6. Retreat Threshold Slider (0% to 75%)
            addDrawableChild(new TacticalSliderWidget(leftX + 112, y + 84, 108, 16, "Retreat", "%", 0.0, 0.75, sliderRetreat, val -> {
                sliderRetreat = val;
                syncSlidersToServer();
            }));

            // Category Cyclers
            addDrawableChild(ButtonWidget.builder(Text.literal("🦁 ⮜ Entity ⮞"), b -> sendButton(hasShiftDown() ? 49 : 50))
                    .dimensions(leftX, y + 102, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🎯 ⮜ Role ⮞"), b -> sendButton(hasShiftDown() ? 48 : 51))
                    .dimensions(leftX + 112, y + 102, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🐎 ⮜ Mount ⮞"), b -> sendButton(hasShiftDown() ? 59 : 55))
                    .dimensions(leftX, y + 120, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🚩 ⮜ Faction ⮞"), b -> sendButton(hasShiftDown() ? 47 : 52))
                    .dimensions(leftX + 112, y + 120, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(currentCommander ? "★ Commander: [YES]" : "☆ Commander: [NO]"), b -> sendButton(54))
                    .dimensions(leftX, y + 138, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("💾 Save Gear"), b -> sendButton(0))
                    .dimensions(leftX + 112, y + 138, 108, 16).build());

            // Right Panel Spawn & Management Bar
            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ Spawn 1x"), b -> sendButton(1))
                    .dimensions(rightX + 90, y + 30, 80, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🛡 Squad (5x)"), b -> sendButton(2))
                    .dimensions(rightX + 90, y + 48, 80, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("➕ Duplicate"), b -> sendButton(4))
                    .dimensions(rightX + 90, y + 66, 80, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 Delete"), b -> sendButton(5))
                    .dimensions(rightX + 90, y + 84, 80, 16).build());

        } else if (tab == 1) { // TAB 1: DEDICATED NEW TROOP CREATOR STUDIO
            int formX = leftX + 10;
            int formW = backgroundWidth - 44;

            newTroopNameField = new TextFieldWidget(textRenderer, formX, y + 46, formW / 2 - 6, 18, Text.literal("Troop Name"));
            newTroopNameField.setPlaceholder(Text.literal("Troop Name (e.g. Frost Valkyrie)"));
            newTroopNameField.setText("Custom Warrior");
            addDrawableChild(newTroopNameField);

            newTroopIdField = new TextFieldWidget(textRenderer, formX + formW / 2 + 6, y + 46, formW / 2 - 6, 18, Text.literal("Troop ID"));
            newTroopIdField.setPlaceholder(Text.literal("Troop ID (e.g. frost_valkyrie)"));
            newTroopIdField.setText("frost_valkyrie");
            addDrawableChild(newTroopIdField);

            // Archetype Template Selector
            addDrawableChild(ButtonWidget.builder(Text.literal("Archetype: " + ARCHETYPE_NAMES[newTroopArchetypeIdx]), b -> {
                newTroopArchetypeIdx = (newTroopArchetypeIdx + 1) % ARCHETYPE_KEYS.length;
                rebuildInterface();
            }).dimensions(formX, y + 74, formW, 18).build());

            // Base Mob Selector
            addDrawableChild(ButtonWidget.builder(Text.literal("Base Mob: " + ENTITY_DISPLAY_NAMES[newTroopEntityIdx]), b -> {
                newTroopEntityIdx = (newTroopEntityIdx + 1) % ENTITY_CHOICES.length;
                rebuildInterface();
            }).dimensions(formX, y + 98, formW / 2 - 6, 18).build());

            // Faction Selector
            addDrawableChild(ButtonWidget.builder(Text.literal("Faction: " + FACTION_NAMES[newTroopFactionIdx]), b -> {
                newTroopFactionIdx = (newTroopFactionIdx + 1) % FACTION_OPTIONS.length;
                rebuildInterface();
            }).dimensions(formX + formW / 2 + 6, y + 98, formW / 2 - 6, 18).build());

            // Big Action Button: FORGE & RECRUIT NEW TROOP
            addDrawableChild(ButtonWidget.builder(Text.literal("✨ FORGE & RECRUIT NEW TROOP ✨").formatted(Formatting.BOLD), b -> createCustomTroopAction())
                    .dimensions(formX, y + 130, formW, 24).build());

            // Preset Quick-Spawn Archetype Shortcuts
            int chipW = (formW - 18) / 4;
            addDrawableChild(ButtonWidget.builder(Text.literal("🛡 Warrior"), b -> quickCreateArchetype("warrior", "Custom Warrior", "minecraft:villager", "kingdom"))
                    .dimensions(formX + 0 * (chipW + 6), y + 164, chipW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🏹 Archer"), b -> quickCreateArchetype("archer", "Custom Archer", "minecraft:skeleton", "kingdom"))
                    .dimensions(formX + 1 * (chipW + 6), y + 164, chipW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🐎 Cavalry"), b -> quickCreateArchetype("cavalry", "Custom Cavalry", "minecraft:villager", "kingdom"))
                    .dimensions(formX + 2 * (chipW + 6), y + 164, chipW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🧙 Mage"), b -> quickCreateArchetype("mage", "Custom Mage", "minecraft:witch", "arcane"))
                    .dimensions(formX + 3 * (chipW + 6), y + 164, chipW, 16).build());

        } else if (tab == 2) { // TAB 2: SAVED UNITS LIBRARY
            searchField = new TextFieldWidget(textRenderer, leftX, y + 32, 224, 18, Text.literal("Search"));
            searchField.setPlaceholder(Text.literal("Search units (knight, wolf, bear, dragoon, titan)..."));
            addDrawableChild(searchField);

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ Spawn Equipped"), b -> sendButton(1))
                    .dimensions(rightX, y + 32, 162, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🛡 Spawn Squad (5)"), b -> sendButton(2))
                    .dimensions(rightX, y + 50, 162, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Cavalry"), b -> sendButton(80))
                    .dimensions(rightX, y + 70, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 War Wolf"), b -> sendButton(81))
                    .dimensions(rightX + 84, y + 70, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Battle Bear"), b -> sendButton(82))
                    .dimensions(rightX, y + 88, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Iron Titan"), b -> sendButton(83))
                    .dimensions(rightX + 84, y + 88, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Dragoon"), b -> sendButton(84))
                    .dimensions(rightX, y + 106, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Paladin"), b -> sendButton(85))
                    .dimensions(rightX + 84, y + 106, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Next Unit"), b -> sendButton(3))
                    .dimensions(rightX, y + 126, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⮜ Prev Unit"), b -> sendButton(6))
                    .dimensions(rightX + 84, y + 126, 78, 16).build());

        } else if (tab == 3) { // TAB 3: FACTIONS & DIPLOMACY
            addDrawableChild(ButtonWidget.builder(Text.literal("Kingdom ↔ Raiders: Toggle"), b -> sendButton(70))
                    .dimensions(rightX, y + 34, 162, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Kingdom ↔ Villagers: Toggle"), b -> sendButton(71))
                    .dimensions(rightX, y + 58, 162, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Raiders ↔ Villagers: Toggle"), b -> sendButton(72))
                    .dimensions(rightX, y + 82, 162, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Restore Defaults"), b -> sendButton(30))
                    .dimensions(rightX, y + 110, 162, 20).build());

        } else if (tab == 4) { // TAB 4: CUSTOM BATTLE SANDBOX
            int colW = 196;
            int col1 = leftX;
            int col2 = leftX + colW + 12;

            // Side A Builder
            addDrawableChild(ButtonWidget.builder(Text.literal("Side A: " + FACTION_NAMES[battleFactionAIdx]), b -> {
                battleFactionAIdx = (battleFactionAIdx + 1) % FACTION_OPTIONS.length;
                rebuildInterface();
            }).dimensions(col1, y + 32, colW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Unit: " + UNIT_NAMES[battleUnitAIdx]), b -> {
                battleUnitAIdx = (battleUnitAIdx + 1) % UNIT_OPTIONS.length;
                rebuildInterface();
            }).dimensions(col1, y + 50, colW, 16).build());

            addDrawableChild(new TacticalSliderWidget(col1, y + 68, colW, 16, "Troops A", "units", 1.0, 50.0, battleCountA, val -> {
                battleCountA = val;
            }));

            // Side B Builder
            addDrawableChild(ButtonWidget.builder(Text.literal("Side B: " + FACTION_NAMES[battleFactionBIdx]), b -> {
                battleFactionBIdx = (battleFactionBIdx + 1) % FACTION_OPTIONS.length;
                rebuildInterface();
            }).dimensions(col2, y + 32, colW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Unit: " + UNIT_NAMES[battleUnitBIdx]), b -> {
                battleUnitBIdx = (battleUnitBIdx + 1) % UNIT_OPTIONS.length;
                rebuildInterface();
            }).dimensions(col2, y + 50, colW, 16).build());

            addDrawableChild(new TacticalSliderWidget(col2, y + 68, colW, 16, "Troops B", "units", 1.0, 50.0, battleCountB, val -> {
                battleCountB = val;
            }));

            // Modifiers: Formation & Distance
            addDrawableChild(ButtonWidget.builder(Text.literal("Formation: " + FORMATION_NAMES[battleFormationIdx]), b -> {
                battleFormationIdx = (battleFormationIdx + 1) % FORMATION_OPTIONS.length;
                rebuildInterface();
            }).dimensions(col1, y + 86, colW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Distance: " + DISTANCE_NAMES[battleDistanceIdx]), b -> {
                battleDistanceIdx = (battleDistanceIdx + 1) % DISTANCE_OPTIONS.length;
                rebuildInterface();
            }).dimensions(col2, y + 86, colW, 16).build());

            // Launch Button
            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ LAUNCH CUSTOM WAR CLASH ⚔"), b -> launchCustomBattle())
                    .dimensions(leftX, y + 104, backgroundWidth - 24, 18).build());

            // Preset Buttons
            int presetW = 96;
            addDrawableChild(ButtonWidget.builder(Text.literal("👑 1 Titan vs 25"), b -> launchPreset("titan_vs_swarm"))
                    .dimensions(leftX + (0 * (presetW + 6)), y + 124, presetW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🐎 12v12 Cavalry"), b -> launchPreset("cavalry_charge"))
                    .dimensions(leftX + (1 * (presetW + 6)), y + 124, presetW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("💀 10v30 Siege"), b -> launchPreset("undead_siege"))
                    .dimensions(leftX + (2 * (presetW + 6)), y + 124, presetW, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚡ 8 vs 24 Smite"), b -> launchPreset("paladin_crusade"))
                    .dimensions(leftX + (3 * (presetW + 6)), y + 124, presetW, 16).build());

            // Utility Controls
            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 Clear Battle Mobs"), b -> sendButton(11))
                    .dimensions(leftX, y + 142, 196, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Reset Stats Scoreboard"), b -> sendButton(12))
                    .dimensions(leftX + 208, y + 142, 196, 16).build());

        } else if (tab == 5) { // TAB 5: SIMULATION CONFIG
            addDrawableChild(ButtonWidget.builder(Text.literal("Building AI: " + (buildingEnabled ? "ON [Active]" : "OFF [Disabled]")), b -> {
                buildingEnabled = !buildingEnabled;
                sendButton(31);
                rebuildInterface();
            }).dimensions(leftX, y + 34, 195, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Potion AI: " + (potionsEnabled ? "ON [Active]" : "OFF [Disabled]")), b -> {
                potionsEnabled = !potionsEnabled;
                sendButton(32);
                rebuildInterface();
            }).dimensions(leftX, y + 58, 195, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Fireworks Artillery: " + (fireworksEnabled ? "ON [Active]" : "OFF [Disabled]")), b -> {
                fireworksEnabled = !fireworksEnabled;
                sendButton(33);
                rebuildInterface();
            }).dimensions(leftX, y + 82, 195, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Shield Defense: " + (shieldEnabled ? "ON [Active]" : "OFF [Disabled]")), b -> {
                shieldEnabled = !shieldEnabled;
                sendButton(34);
                rebuildInterface();
            }).dimensions(leftX, y + 106, 195, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Friendly Fire: " + (friendlyFireAllowed ? "ALLOWED" : "PREVENTED")), b -> {
                friendlyFireAllowed = !friendlyFireAllowed;
                sendButton(35);
                rebuildInterface();
            }).dimensions(leftX, y + 130, 195, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Reset Everything to Defaults"), b -> sendButton(30))
                    .dimensions(rightX, y + 34, 162, 22).build());
        }
    }

    private void createCustomTroopAction() {
        String name = newTroopNameField != null ? newTroopNameField.getText() : "Custom Troop";
        String id = newTroopIdField != null ? newTroopIdField.getText() : "custom_troop";
        String template = ARCHETYPE_KEYS[newTroopArchetypeIdx];
        String entityId = ENTITY_CHOICES[newTroopEntityIdx];
        String factionId = FACTION_OPTIONS[newTroopFactionIdx];

        ClientPlayNetworking.send(new ModPackets.CreateCustomTroopPayload(
                id, name, entityId, factionId, "melee", template
        ));
        tab = 0; // Switch to editor to see the new unit immediately
        rebuildInterface();
    }

    private void quickCreateArchetype(String template, String name, String entityId, String factionId) {
        String id = template + "_" + System.currentTimeMillis() % 10000;
        ClientPlayNetworking.send(new ModPackets.CreateCustomTroopPayload(
                id, name, entityId, factionId, "melee", template
        ));
        tab = 0;
        rebuildInterface();
    }

    private void launchCustomBattle() {
        String fA = FACTION_OPTIONS[battleFactionAIdx];
        String uA = UNIT_OPTIONS[battleUnitAIdx];
        int cA = (int) Math.round(battleCountA);

        String fB = FACTION_OPTIONS[battleFactionBIdx];
        String uB = UNIT_OPTIONS[battleUnitBIdx];
        int cB = (int) Math.round(battleCountB);

        String form = FORMATION_OPTIONS[battleFormationIdx];
        float dist = DISTANCE_OPTIONS[battleDistanceIdx];

        ClientPlayNetworking.send(new ModPackets.StartCustomBattlePayload(
                fA, uA, cA, fB, uB, cB, form, dist
        ));
        close();
    }

    private void launchPreset(String preset) {
        if ("titan_vs_swarm".equals(preset)) {
            ClientPlayNetworking.send(new ModPackets.StartCustomBattlePayload("villagers", "iron_titan", 1, "raiders", "raider_berserker", 25, "ambush", 24.0f));
        } else if ("cavalry_charge".equals(preset)) {
            ClientPlayNetworking.send(new ModPackets.StartCustomBattlePayload("kingdom", "royal_cavalry", 12, "undead", "dread_cavalry", 12, "line", 40.0f));
        } else if ("undead_siege".equals(preset)) {
            ClientPlayNetworking.send(new ModPackets.StartCustomBattlePayload("villagers", "all", 10, "undead", "all", 30, "ambush", 28.0f));
        } else if ("paladin_crusade".equals(preset)) {
            ClientPlayNetworking.send(new ModPackets.StartCustomBattlePayload("kingdom", "holy_paladin", 8, "undead", "bogged_sniper", 24, "line", 30.0f));
        }
        close();
    }

    private void syncSlidersToServer() {
        ClientPlayNetworking.send(new ModPackets.UpdateUnitAttributesPayload(
                (float) sliderHealth,
                (float) sliderDamage,
                (float) sliderArmor,
                (float) sliderScale,
                (float) sliderSpeed,
                (float) sliderRetreat
        ));
    }

    private void sendButton(int id) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, id);
        }
    }

    private LivingEntity getOrCreateDummyEntity(String entityIdStr) {
        if (client == null || client.world == null) return null;
        return dummyEntityCache.computeIfAbsent(entityIdStr, id -> {
            try {
                Identifier idObj = Identifier.tryParse(id);
                if (idObj == null) idObj = Identifier.of("minecraft", "villager");
                var entityType = Registries.ENTITY_TYPE.get(idObj);
                if (entityType != null) {
                    var ent = entityType.create(client.world);
                    if (ent instanceof LivingEntity living) {
                        return living;
                    }
                }
            } catch (Throwable ignored) {}
            return null;
        });
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // High-tech military command backdrop
        context.fill(0, 0, width, height, COLOR_BG_BACKDROP);

        // Main Dialog Console Panel
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, COLOR_PANEL_MAIN);
        context.fill(x + 6, y + 26, x + backgroundWidth - 6, y + backgroundHeight - 6, COLOR_PANEL_INNER);

        // Header separator line
        context.fill(x + 6, y + 26, x + backgroundWidth - 6, y + 27, COLOR_CARD_BORDER);

        // Tab selection highlight under active tab
        int tabWidth = 66;
        context.fill(x + 8 + (tab * (tabWidth + 3)), y + 24, x + 8 + (tab * (tabWidth + 3)) + tabWidth, y + 26, COLOR_GOLD_ACCENT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        drawBackground(context, delta, mouseX, mouseY);
        drawTabContent(context, mouseX, mouseY);

        // Draw slots only on Tab 0 (Unit Editor)
        if (tab == 0) {
            super.render(context, mouseX, mouseY, delta);
            drawSlotLabels(context);
        } else {
            // Draw widgets
            for (var child : this.children()) {
                if (child instanceof net.minecraft.client.gui.Drawable drawable) {
                    drawable.render(context, mouseX, mouseY, delta);
                }
            }
        }

        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void drawSlotLabels(DrawContext context) {
        int eqX = x + 255;
        int eqY = y + 128;
        context.drawText(textRenderer, Text.literal("H   C   L   F   M   O").formatted(Formatting.DARK_GRAY), eqX + 3, eqY, 0xff8294ad, false);
        context.drawText(textRenderer, Text.literal("Equipment & Satchel").formatted(Formatting.AQUA), eqX, y + 116, 0xff9ecbff, false);
    }

    private void drawTabContent(DrawContext context, int mouseX, int mouseY) {
        int leftX = x + 12;

        if (tab == 0) { // TAB 0: LIVE 3D PREVIEW & COMBAT PROFILE
            // Draw 3D Entity Preview Box
            int prevX = x + 250;
            int prevY = y + 30;
            int prevW = 82;
            int prevH = 74;

            context.fill(prevX, prevY, prevX + prevW, prevY + prevH, COLOR_CARD_BG);
            context.drawBorder(prevX, prevY, prevW, prevH, COLOR_CARD_BORDER);

            LivingEntity dummy = getOrCreateDummyEntity(currentEntityId);
            if (dummy != null) {
                // Synchronize equipment slots to preview model
                for (int i = 0; i < 6; i++) {
                    ItemStack stack = handler.equipmentInventory.getStack(i);
                    dummy.equipStack(CreatorScreenHandler.EQUIPMENT_SLOTS[i], stack);
                }
                int entitySize = (int) (28 * Math.max(0.6f, Math.min(1.8f, (float) sliderScale)));
                InventoryScreen.drawEntity(context, prevX + 2, prevY + 2, prevX + prevW - 2, prevY + prevH - 2, entitySize, 0.0625f, (float) mouseX, (float) mouseY, dummy);
            }

            // Unit Identity Header above stats
            int leftY = y + 158;
            context.fill(leftX, leftY - 4, leftX + 224, y + backgroundHeight - 8, COLOR_CARD_BG);
            context.fill(leftX, leftY - 4, leftX + 3, y + backgroundHeight - 8, COLOR_GOLD_ACCENT);

            String titleStr = "⚔ " + currentUnitName + " [" + (currentUnitIndex + 1) + "/" + Math.max(1, totalUnitCount) + "]";
            context.drawText(textRenderer, Text.literal(titleStr).formatted(Formatting.GOLD, Formatting.BOLD), leftX + 8, leftY, COLOR_GOLD_ACCENT, false);

            drawStatBar(context, leftX + 8, leftY + 12, "❤ HP", (float) sliderHealth, 300.0f, 0xffef4444);
            drawStatBar(context, leftX + 8, leftY + 24, "🗡 DMG", (float) sliderDamage, 50.0f, 0xfff97316);
            drawStatBar(context, leftX + 8, leftY + 36, "🛡 ARM", (float) sliderArmor, 30.0f, 0xff38bdf8);
            drawStatBar(context, leftX + 8, leftY + 48, "⚡ SPD", (float) (sliderSpeed * 100), 60.0f, 0xffeab308);
            drawStatBar(context, leftX + 8, leftY + 60, "📏 SCL", (float) sliderScale, 3.0f, 0xffa855f7);

            String infoBadge = "• " + currentFactionId.toUpperCase() + " | " + currentRole.toUpperCase() + (currentCommander ? " | ★ CMD" : "");
            context.drawText(textRenderer, Text.literal(infoBadge).formatted(Formatting.DARK_GRAY), leftX + 8, leftY + 74, 0xff94a3b8, false);

        } else if (tab == 1) { // TAB 1: NEW TROOP CREATOR STUDIO
            int formX = leftX + 10;
            context.drawTextWithShadow(textRenderer, Text.literal("TROOP CREATION STUDIO").formatted(Formatting.BOLD, Formatting.GOLD), formX, y + 32, 0xffffd700);
            context.drawText(textRenderer, Text.literal("Type a custom name and select starting archetype, mob type, and faction:").formatted(Formatting.GRAY), formX, y + 118, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("Quick Starter Presets (Instant Forge):").formatted(Formatting.AQUA), formX, y + 152, 0xff38bdf8, false);

        } else if (tab == 2) { // TAB 2: UNITS LIBRARY
            int cardY = y + 54;
            String q = searchField == null ? "" : searchField.getText().toLowerCase();
            int cardsDrawn = 0;

            String[][] allUnits = {
                    {"Royal Knight", "VILLAGER • MELEE TANK • 40 HP • 7.5 DMG", "0xff3b82f6", "royal knight villager melee tank", "royal_knight"},
                    {"Royal Heavy Cavalry", "HORSE MOUNT • ARMORED LANCE • 45 HP", "0xff3b82f6", "royal cavalry horse mount", "royal_cavalry"},
                    {"Savage War Wolf", "WOLF • BERSERKER BEAST • 36 HP • 8.0 DMG", "0xfff97316", "war wolf beast berserker animal canine naturalist", "war_wolf"},
                    {"Armored Battle Bear", "POLAR BEAR • 1.25x TANK JUGGERNAUT • 85 HP", "0xff38bdf8", "battle bear polar grizzly bear naturalist tank", "battle_bear"},
                    {"Desert Camel Dragoon", "CAMEL MOUNT • CROSSBOW RANGED • 48 HP", "0xffeab308", "desert camel dragoon mount ranged marksman", "camel_dragoon"},
                    {"Royal Archer", "SKELETON • RANGED SNIPER • 24 HP • 5.0 DMG", "0xff38bdf8", "royal archer skeleton ranged", "royal_archer"},
                    {"Field Medic", "VILLAGER • HEALER • 28 HP • POTIONS", "0xff10b981", "field medic villager healer support potion", "field_medic"},
                    {"Holy Paladin Crusader", "PALADIN • SMITE & ABSORPTION • 55 HP", "0xfffacc15", "holy paladin crusader mace sunforge", "holy_paladin"},
                    {"Colossal Iron Titan", "IRON GOLEM • 1.35x SCALE • 150 HP • 16 DMG", "0xff10b981", "colossal iron titan golem tank warlord", "iron_titan"},
                    {"Grove Druid", "VILLAGER • ROOT SNARE & NATURE • 32 HP", "0xff22c55e", "grove druid nature roots wolf", "nature_druid"},
                    {"Royal Pyrotechnic", "PILLAGER • FIREWORKS ARTILLERY • 30 HP", "0xffef4444", "royal pyro pillager fireworks artillery explosive", "royal_pyro"},
                    {"Siege Bombardier", "PIGLIN • TNT MORTAR SAPPER • 35 HP", "0xfff97316", "siege bombardier piglin mortar tnt", "siege_bombardier"},
                    {"Raider Berserker", "PIGLIN BRUTE • BLOODRAGE • 40 HP", "0xffef4444", "raider berserker piglin brute fury", "raider_berserker"},
                    {"Undead Necromancer", "EVOKER • DARK SUMMONER • 45 HP", "0xff8b5cf6", "undead necromancer evoker summoner", "undead_necromancer"},
                    {"Shadowfang Assassin", "STRAY • BLINK BACKSTAB • 28 HP", "0xffa855f7", "shadow assassin stray dagger blink", "shadow_assassin"},
                    {"Dread Skeleton Cavalry", "SKELETON HORSE • WITHER • 48 HP", "0xff6b21a8", "dread cavalry wither skeleton horse", "dread_cavalry"},
                    {"Mossy Bogged Sniper", "BOGGED • POISON SNIPER • 26 HP", "0xff84cc16", "bogged poison sniper archer", "poison_bogged"},
                    {"Tempest Breeze", "BREEZE • WIND GUST AOE • 40 HP", "0xff06b6d4", "tempest breeze wind knockback", "tempest_breeze"},
                    {"Celestial Pixie Medic", "ALLAY • FLYING HEALER • 25 HP", "0xff38bdf8", "celestial pixie medic allay flying fairy", "fairy_medic"},
                    {"Arcane Bard", "VILLAGER • WAR LUTE BUFFS • 30 HP", "0xff06b6d4", "arcane bard lute music buff", "arcane_bard"},
                    {"Combat Engineer", "VILLAGER • BARRICADE BUILDER • 32 HP", "0xff0ea5e9", "combat engineer villager builder barricade", "combat_engineer"}
            };

            for (String[] u : allUnits) {
                if (cardsDrawn >= 5) break;
                if (matches(q, u[3])) {
                    int color = (int) Long.parseLong(u[2].replace("0x", ""), 16);
                    drawUnitCard(context, leftX, cardY, u[0], u[1], color);
                    cardY += 34;
                    cardsDrawn++;
                }
            }

        } else if (tab == 3) { // TAB 3: DIPLOMACY
            drawFactionCard(context, leftX, y + 34, "Kingdom of Eldoria", "Blue (#3B82F6) • 6 Units • Hostile: Raiders, Undead", 0xff3b82f6);
            drawFactionCard(context, leftX, y + 72, "Iron Raiders", "Red (#EF4444) • 3 Units • Hostile: Kingdom, Village", 0xffef4444);
            drawFactionCard(context, leftX, y + 110, "Village Alliance", "Green (#10B981) • 3 Units • Allied: Kingdom", 0xff10b981);

            int infoY = y + 152;
            context.drawTextWithShadow(textRenderer, Text.literal("FACTION PERKS & FRIENDLY FIRE DIPLOMACY").formatted(Formatting.BOLD, Formatting.GOLD), leftX, infoY, 0xffffd700);
            context.drawText(textRenderer, Text.literal("• Military Discipline (+15% DMG)  • Heavy Armor (+20% Armor)").formatted(Formatting.WHITE), leftX, infoY + 14, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• Pyrotechnics (+50% Fireworks AoE) • Holy Might (+50% Heals)").formatted(Formatting.WHITE), leftX, infoY + 26, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• Zero friendly fire damage between allied or same-faction forces.").formatted(Formatting.GREEN), leftX, infoY + 38, 0xff4ade80, false);

        } else if (tab == 4) { // TAB 4: BATTLE MATCHUP BANNER
            int bannerY = y + 164;
            context.fill(leftX, bannerY, leftX + backgroundWidth - 24, y + backgroundHeight - 8, COLOR_CARD_BG);
            context.fill(leftX, bannerY, leftX + 4, y + backgroundHeight - 8, COLOR_GOLD_ACCENT);

            int countA = (int) Math.round(battleCountA);
            int countB = (int) Math.round(battleCountB);
            String summaryA = countA + "x " + UNIT_NAMES[battleUnitAIdx] + " (" + FACTION_NAMES[battleFactionAIdx] + ")";
            String summaryB = countB + "x " + UNIT_NAMES[battleUnitBIdx] + " (" + FACTION_NAMES[battleFactionBIdx] + ")";

            context.drawText(textRenderer, Text.literal("⚔ TACTICAL SIMULATION MATCHUP:").formatted(Formatting.GOLD, Formatting.BOLD), leftX + 8, bannerY + 4, COLOR_GOLD_ACCENT, false);
            context.drawText(textRenderer, Text.literal("Side A: " + summaryA).formatted(Formatting.AQUA), leftX + 8, bannerY + 16, 0xff38bdf8, false);
            context.drawText(textRenderer, Text.literal("Side B: " + summaryB).formatted(Formatting.RED), leftX + 8, bannerY + 28, 0xfff87171, false);

            int meterX = leftX + 8;
            int meterY = bannerY + 40;
            int meterWidth = backgroundWidth - 40;
            float ratio = (float) countA / Math.max(1, countA + countB);
            int splitW = (int) (meterWidth * ratio);

            context.fill(meterX, meterY, meterX + splitW, meterY + 6, 0xff3b82f6);
            context.fill(meterX + splitW, meterY, meterX + meterWidth, meterY + 6, 0xffef4444);
            context.fill(meterX + splitW - 1, meterY - 1, meterX + splitW + 1, meterY + 7, 0xffffffff);

            context.drawText(textRenderer, Text.literal("Power Ratio: " + (int)(ratio * 100) + "% vs " + (100 - (int)(ratio * 100)) + "%").formatted(Formatting.DARK_GRAY), meterX, meterY + 10, 0xff94a3b8, false);

        } else if (tab == 5) { // TAB 5: CONFIG
            int infoX = x + 218;
            int infoY = y + 68;
            context.drawTextWithShadow(textRenderer, Text.literal("WORLD CONFIG & AUTHORITY").formatted(Formatting.BOLD, Formatting.GOLD), infoX, infoY, 0xffffd700);
            context.drawText(textRenderer, Text.literal("• 100% Server Authoritative Architecture").formatted(Formatting.WHITE), infoX, infoY + 16, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• AI runs throttled every 10 server ticks").formatted(Formatting.GRAY), infoX, infoY + 28, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("• Spatial queries protect TPS performance").formatted(Formatting.GRAY), infoX, infoY + 40, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("• Full data persistence across restarts").formatted(Formatting.GREEN), infoX, infoY + 52, 0xff4ade80, false);
        }
    }

    private void drawStatBar(DrawContext context, int barX, int barY, String label, float val, float maxVal, int barColor) {
        context.drawText(textRenderer, Text.literal(label), barX, barY, 0xffcbd5e1, false);
        int trackX = barX + 50;
        int trackW = 150;
        context.fill(trackX, barY + 2, trackX + trackW, barY + 7, 0xff0f172a);
        int fillW = Math.max(0, Math.min(trackW, (int) ((val / maxVal) * trackW)));
        context.fill(trackX, barY + 2, trackX + fillW, barY + 7, barColor);
    }

    private boolean matches(String query, String tags) {
        if (query.isBlank()) return true;
        for (String part : query.split(" ")) {
            if (!part.isBlank() && tags.contains(part)) return true;
        }
        return false;
    }

    private void drawUnitCard(DrawContext context, int cx, int cy, String name, String meta, int accentColor) {
        context.fill(cx, cy, cx + 224, cy + 30, COLOR_CARD_BG);
        context.fill(cx, cy, cx + 3, cy + 30, accentColor);
        context.drawText(textRenderer, Text.literal(name).formatted(Formatting.BOLD), cx + 8, cy + 4, 0xfff8fafc, false);
        context.drawText(textRenderer, Text.literal(meta), cx + 8, cy + 16, 0xff94a3b8, false);
    }

    private void drawFactionCard(DrawContext context, int cx, int cy, String name, String meta, int accentColor) {
        context.fill(cx, cy, cx + 224, cy + 34, COLOR_CARD_BG);
        context.fill(cx, cy, cx + 4, cy + 34, accentColor);
        context.drawText(textRenderer, Text.literal(name).formatted(Formatting.BOLD), cx + 8, cy + 5, 0xfff8fafc, false);
        context.drawText(textRenderer, Text.literal(meta), cx + 8, cy + 18, 0xff94a3b8, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField != null && searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (newTroopNameField != null && newTroopNameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (newTroopIdField != null && newTroopIdField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) { // ESC key
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Interactive Slider Widget for Live Attribute Customization
    private class TacticalSliderWidget extends SliderWidget {
        private final String label;
        private final String unit;
        private final double min;
        private final double max;
        private final java.util.function.Consumer<Double> onValueChange;

        public TacticalSliderWidget(int x, int y, int width, int height, String label, String unit, double min, double max, double current, java.util.function.Consumer<Double> onValueChange) {
            super(x, y, width, height, Text.empty(), (current - min) / (max - min));
            this.label = label;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.onValueChange = onValueChange;
            updateMessage();
        }

        public double getActualValue() {
            return min + (value * (max - min));
        }

        @Override
        protected void updateMessage() {
            double actual = getActualValue();
            String formatted = actual >= 1.0 ? String.format("%.0f", actual) : String.format("%.2f", actual);
            setMessage(Text.literal(label + ": " + formatted + " " + unit));
        }

        @Override
        protected void applyValue() {
            updateMessage();
            if (onValueChange != null) {
                onValueChange.accept(getActualValue());
            }
        }
    }
}
