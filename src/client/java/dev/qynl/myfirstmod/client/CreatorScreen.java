package dev.qynl.myfirstmod.client;

import dev.qynl.myfirstmod.entity.DynamicEntityRegistry;
import dev.qynl.myfirstmod.gui.CreatorScreenHandler;
import dev.qynl.myfirstmod.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CreatorScreen extends HandledScreen<CreatorScreenHandler> {
    private int tab = 0;
    private TextFieldWidget searchField;

    // Tactical Military Command Palette Colors
    private static final int COLOR_BG_BACKDROP = 0xd9060b14;
    private static final int COLOR_PANEL_MAIN = 0xff0f172a;
    private static final int COLOR_PANEL_INNER = 0xff090e1a;
    private static final int COLOR_CARD_BG = 0xff1e293b;
    private static final int COLOR_CARD_HOVER = 0xff334155;
    private static final int COLOR_CARD_BORDER = 0xff475569;
    private static final int COLOR_GOLD_ACCENT = 0xfff59e0b;
    private static final int COLOR_CYAN_ACCENT = 0xff06b6d4;
    private static final int COLOR_GREEN_ACCENT = 0xff10b981;
    private static final int COLOR_RED_ACCENT = 0xffef4444;

    // Live Slider Values (Interactive Solo Editing)
    private double sliderHealth = 40.0;
    private double sliderDamage = 7.5;
    private double sliderArmor = 15.0;
    private double sliderScale = 1.0;
    private double sliderSpeed = 0.25;
    private double sliderRetreat = 0.20;

    // Simulation toggle states for UI display
    private boolean buildingEnabled = true;
    private boolean potionsEnabled = true;
    private boolean fireworksEnabled = true;
    private boolean shieldEnabled = true;
    private boolean friendlyFireAllowed = false;

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

    private void rebuildInterface() {
        clearChildren();

        int topY = y + 7;
        int tabWidth = 78;

        // Navigation Tab Bar with active highlighted styling
        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 0 ? "⚔ [EDITOR]" : "⚔ Editor"), b -> { tab = 0; rebuildInterface(); })
                .dimensions(x + 10 + (0 * (tabWidth + 4)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 1 ? "📋 [UNITS]" : "📋 Units"), b -> { tab = 1; rebuildInterface(); })
                .dimensions(x + 10 + (1 * (tabWidth + 4)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 2 ? "🛡 [FACTIONS]" : "🛡 Factions"), b -> { tab = 2; rebuildInterface(); })
                .dimensions(x + 10 + (2 * (tabWidth + 4)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 3 ? "💥 [BATTLE]" : "💥 Battle"), b -> { tab = 3; rebuildInterface(); })
                .dimensions(x + 10 + (3 * (tabWidth + 4)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 4 ? "⚙ [CONFIG]" : "⚙ Config"), b -> { tab = 4; rebuildInterface(); })
                .dimensions(x + 10 + (4 * (tabWidth + 4)), topY, tabWidth, 20).build());

        int leftX = x + 14;
        int rightX = x + 252;

        if (tab == 0) { // UNIT EDITOR WITH REAL-TIME SLIDERS
            // Unit Selectors
            addDrawableChild(ButtonWidget.builder(Text.literal("⮜ Prev Unit"), b -> sendButton(6))
                    .dimensions(leftX, y + 32, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Next Unit ⮞"), b -> sendButton(3))
                    .dimensions(leftX + 112, y + 32, 108, 16).build());

            // 1. Health Slider (5 to 300 HP)
            addDrawableChild(new TacticalSliderWidget(leftX, y + 50, 108, 16, "Health", "HP", 5.0, 300.0, sliderHealth, val -> {
                sliderHealth = val;
                syncSlidersToServer();
            }));

            // 2. Attack Damage Slider (1 to 50 DMG)
            addDrawableChild(new TacticalSliderWidget(leftX + 112, y + 50, 108, 16, "Damage", "DMG", 1.0, 50.0, sliderDamage, val -> {
                sliderDamage = val;
                syncSlidersToServer();
            }));

            // 3. Armor Slider (0 to 30 Armor)
            addDrawableChild(new TacticalSliderWidget(leftX, y + 68, 108, 16, "Armor", "pts", 0.0, 30.0, sliderArmor, val -> {
                sliderArmor = val;
                syncSlidersToServer();
            }));

            // 4. Visual Scale Slider (0.25x to 3.00x)
            addDrawableChild(new TacticalSliderWidget(leftX + 112, y + 68, 108, 16, "Scale", "x", 0.25, 3.0, sliderScale, val -> {
                sliderScale = val;
                syncSlidersToServer();
            }));

            // 5. Movement Speed Slider (0.10 to 0.60)
            addDrawableChild(new TacticalSliderWidget(leftX, y + 86, 108, 16, "Speed", "spd", 0.10, 0.60, sliderSpeed, val -> {
                sliderSpeed = val;
                syncSlidersToServer();
            }));

            // 6. Retreat Threshold Slider (0% to 75%)
            addDrawableChild(new TacticalSliderWidget(leftX + 112, y + 86, 108, 16, "Retreat", "%", 0.0, 0.75, sliderRetreat, val -> {
                sliderRetreat = val;
                syncSlidersToServer();
            }));

            // Quick Category Cyclers
            addDrawableChild(ButtonWidget.builder(Text.literal("🦁 ⮜ Entity ⮞"), b -> sendButton(hasShiftDown() ? 49 : 50))
                    .dimensions(leftX, y + 104, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🎯 ⮜ Role ⮞"), b -> sendButton(hasShiftDown() ? 48 : 51))
                    .dimensions(leftX + 112, y + 104, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🐎 ⮜ Mount ⮞"), b -> sendButton(hasShiftDown() ? 59 : 55))
                    .dimensions(leftX, y + 122, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🚩 ⮜ Faction ⮞"), b -> sendButton(hasShiftDown() ? 47 : 52))
                    .dimensions(leftX + 112, y + 122, 108, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("★ Commander Status: Toggle"), b -> sendButton(54))
                    .dimensions(leftX, y + 140, 220, 16).build());

            // Right Panel: Solo Faction Quick-Deployment Bar
            addDrawableChild(ButtonWidget.builder(Text.literal("💾 Save Unit Slots"), b -> sendButton(0))
                    .dimensions(rightX, y + 32, 162, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔵 Kingdom: 1x"), b -> spawnSoloFaction("kingdom", 1))
                    .dimensions(rightX, y + 50, 78, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("🔵 Squad (5)"), b -> spawnSoloFaction("kingdom", 5))
                    .dimensions(rightX + 84, y + 50, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔴 Raiders: 1x"), b -> spawnSoloFaction("raiders", 1))
                    .dimensions(rightX, y + 68, 78, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("🔴 Squad (5)"), b -> spawnSoloFaction("raiders", 5))
                    .dimensions(rightX + 84, y + 68, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🟢 Village: 1x"), b -> spawnSoloFaction("villagers", 1))
                    .dimensions(rightX, y + 86, 78, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("🟢 Squad (5)"), b -> spawnSoloFaction("villagers", 5))
                    .dimensions(rightX + 84, y + 86, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🟣 Undead: 1x"), b -> spawnSoloFaction("undead", 1))
                    .dimensions(rightX, y + 104, 78, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("🟣 Squad (5)"), b -> spawnSoloFaction("undead", 5))
                    .dimensions(rightX + 84, y + 104, 78, 16).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("➕ Duplicate"), b -> sendButton(4))
                    .dimensions(rightX, y + 122, 78, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 Delete"), b -> sendButton(5))
                    .dimensions(rightX + 84, y + 122, 78, 16).build());

        } else if (tab == 1) { // SAVED UNITS LIBRARY
            searchField = new TextFieldWidget(textRenderer, leftX, y + 34, 224, 20, Text.literal("Search"));
            searchField.setPlaceholder(Text.literal("Search units (knight, wolf, bear, dragoon, titan)..."));
            addDrawableChild(searchField);

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ Spawn Equipped"), b -> sendButton(1))
                    .dimensions(rightX, y + 34, 162, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🛡 Spawn Squad (5)"), b -> sendButton(2))
                    .dimensions(rightX, y + 54, 162, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Cavalry"), b -> sendButton(80))
                    .dimensions(rightX, y + 74, 78, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 War Wolf"), b -> sendButton(81))
                    .dimensions(rightX + 84, y + 74, 78, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Battle Bear"), b -> sendButton(82))
                    .dimensions(rightX, y + 94, 78, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Iron Titan"), b -> sendButton(83))
                    .dimensions(rightX + 84, y + 94, 78, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Dragoon"), b -> sendButton(84))
                    .dimensions(rightX, y + 114, 78, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("📥 Paladin"), b -> sendButton(85))
                    .dimensions(rightX + 84, y + 114, 78, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Next Unit"), b -> sendButton(3))
                    .dimensions(rightX, y + 134, 78, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⮜ Prev Unit"), b -> sendButton(6))
                    .dimensions(rightX + 84, y + 134, 78, 18).build());

        } else if (tab == 2) { // FACTIONS & DIPLOMACY
            addDrawableChild(ButtonWidget.builder(Text.literal("Kingdom ↔ Raiders: Toggle"), b -> sendButton(70))
                    .dimensions(rightX, y + 34, 162, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Kingdom ↔ Villagers: Toggle"), b -> sendButton(71))
                    .dimensions(rightX, y + 58, 162, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Raiders ↔ Villagers: Toggle"), b -> sendButton(72))
                    .dimensions(rightX, y + 82, 162, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Restore Defaults"), b -> sendButton(30))
                    .dimensions(rightX, y + 110, 162, 20).build());

        } else if (tab == 3) { // BATTLE SANDBOX
            addDrawableChild(ButtonWidget.builder(Text.literal("Faction A: Cycle"), b -> sendButton(65))
                    .dimensions(leftX, y + 34, 108, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Faction B: Cycle"), b -> sendButton(66))
                    .dimensions(leftX + 116, y + 34, 108, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ QUICK CLASH (8 vs 8) ⚔"), b -> sendButton(10))
                    .dimensions(leftX, y + 54, 224, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ LARGE BATTLE (16 vs 16) ⚔"), b -> sendButton(20))
                    .dimensions(leftX, y + 74, 224, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ EPIC WARFARE (24 vs 24) ⚔"), b -> sendButton(21))
                    .dimensions(leftX, y + 94, 224, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ MEGA WAR (32 vs 32) ⚔"), b -> sendButton(22))
                    .dimensions(leftX, y + 114, 224, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ TITAN CLASH (48 vs 48) ⚔"), b -> sendButton(23))
                    .dimensions(leftX, y + 134, 224, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 CLEAR ALL BATTLE MOBS"), b -> sendButton(11))
                    .dimensions(rightX, y + 104, 162, 22).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Reset Battle Stats"), b -> sendButton(12))
                    .dimensions(rightX, y + 130, 162, 22).build());

        } else if (tab == 4) { // SETTINGS & SIMULATION
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

    private void spawnSoloFaction(String factionId, int count) {
        ClientPlayNetworking.send(new ModPackets.SpawnFactionUnitPayload(factionId, count));
    }

    private void sendButton(int id) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, id);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // High-tech military command backdrop
        context.fill(0, 0, width, height, COLOR_BG_BACKDROP);

        // Main Dialog Console Panel
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, COLOR_PANEL_MAIN);
        context.fill(x + 8, y + 28, x + backgroundWidth - 8, y + backgroundHeight - 8, COLOR_PANEL_INNER);

        // Header separator line
        context.fill(x + 8, y + 28, x + backgroundWidth - 8, y + 29, COLOR_CARD_BORDER);

        // Tab selection highlight under active tab
        int tabWidth = 78;
        context.fill(x + 10 + (tab * (tabWidth + 4)), y + 26, x + 10 + (tab * (tabWidth + 4)) + tabWidth, y + 28, COLOR_GOLD_ACCENT);
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
        int eqY = y + 144;
        context.drawText(textRenderer, Text.literal("H   C   L   F   M   O").formatted(Formatting.DARK_GRAY), eqX + 3, eqY, 0xff8294ad, false);
        context.drawText(textRenderer, Text.literal("Unit Equipment & Inventory").formatted(Formatting.AQUA), eqX, y + 130, 0xff9ecbff, false);
    }

    private void drawTabContent(DrawContext context, int mouseX, int mouseY) {
        int leftX = x + 14;
        int leftY = y + 162;

        if (tab == 0) { // UNIT EDITOR CONTENT & STAT HUD
            context.fill(leftX, leftY - 4, leftX + 220, y + backgroundHeight - 12, COLOR_CARD_BG);
            context.fill(leftX, leftY - 4, leftX + 3, y + backgroundHeight - 12, COLOR_GOLD_ACCENT);

            context.drawText(textRenderer, Text.literal("⚔ SOLO SANDBOX COMMANDER ACTIVE").formatted(Formatting.GOLD, Formatting.BOLD), leftX + 8, leftY, COLOR_GOLD_ACCENT, false);
            context.drawText(textRenderer, Text.literal("• Drag sliders to edit stats in real-time").formatted(Formatting.WHITE), leftX + 8, leftY + 14, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• Spawn for any faction instantly with right buttons").formatted(Formatting.GREEN), leftX + 8, leftY + 26, 0xff4ade80, false);
            context.drawText(textRenderer, Text.literal("• Pit blue knights vs red raiders anywhere in solo!").formatted(Formatting.YELLOW), leftX + 8, leftY + 38, 0xfffde047, false);
            context.drawText(textRenderer, Text.literal("• Shift-Right-Click creator tool to cycle spawn faction").formatted(Formatting.GRAY), leftX + 8, leftY + 50, 0xff94a3b8, false);

        } else if (tab == 1) { // SAVED UNITS LIBRARY
            int cardY = y + 58;
            String q = searchField == null ? "" : searchField.getText().toLowerCase();
            int cardsDrawn = 0;

            String[][] allUnits = {
                    {"Royal Knight", "VILLAGER • MELEE TANK • 40 HP • 7.5 DMG", "0xff3b82f6", "royal knight villager melee tank"},
                    {"Royal Heavy Cavalry", "HORSE MOUNT • ARMORED LANCE • 45 HP", "0xff3b82f6", "royal cavalry horse mount"},
                    {"Savage War Wolf", "WOLF • BERSERKER BEAST • 36 HP • 8.0 DMG", "0xfff97316", "war wolf beast berserker animal canine naturalist"},
                    {"Armored Battle Bear", "POLAR BEAR • 1.25x TANK JUGGERNAUT • 85 HP", "0xff38bdf8", "battle bear polar grizzly bear naturalist tank"},
                    {"Desert Camel Dragoon", "CAMEL MOUNT • CROSSBOW RANGED • 48 HP", "0xffeab308", "desert camel dragoon mount ranged marksman"},
                    {"Royal Archer", "SKELETON • RANGED SNIPER • 24 HP • 5.0 DMG", "0xff38bdf8", "royal archer skeleton ranged"},
                    {"Field Medic", "VILLAGER • HEALER • 28 HP • POTIONS", "0xff10b981", "field medic villager healer support potion"},
                    {"Holy Paladin Crusader", "PALADIN • SMITE & ABSORPTION • 55 HP", "0xfffacc15", "holy paladin crusader mace sunforge"},
                    {"Colossal Iron Titan", "IRON GOLEM • 1.35x SCALE • 150 HP • 16 DMG", "0xff10b981", "colossal iron titan golem tank warlord"},
                    {"Grove Druid", "VILLAGER • ROOT SNARE & NATURE • 32 HP", "0xff22c55e", "grove druid nature roots wolf"},
                    {"Royal Pyrotechnic", "PILLAGER • FIREWORKS ARTILLERY • 30 HP", "0xffef4444", "royal pyro pillager fireworks artillery explosive"},
                    {"Siege Bombardier", "PIGLIN • TNT MORTAR SAPPER • 35 HP", "0xfff97316", "siege bombardier piglin mortar tnt"},
                    {"Raider Berserker", "PIGLIN BRUTE • BLOODRAGE • 40 HP", "0xffef4444", "raider berserker piglin brute fury"},
                    {"Undead Necromancer", "EVOKER • DARK SUMMONER • 45 HP", "0xff8b5cf6", "undead necromancer evoker summoner"},
                    {"Shadowfang Assassin", "STRAY • BLINK BACKSTAB • 28 HP", "0xffa855f7", "shadow assassin stray dagger blink"},
                    {"Dread Skeleton Cavalry", "SKELETON HORSE • WITHER • 48 HP", "0xff6b21a8", "dread cavalry wither skeleton horse"},
                    {"Mossy Bogged Sniper", "BOGGED • POISON SNIPER • 26 HP", "0xff84cc16", "bogged poison sniper archer"},
                    {"Tempest Breeze", "BREEZE • WIND GUST AOE • 40 HP", "0xff06b6d4", "tempest breeze wind knockback"},
                    {"Celestial Pixie Medic", "ALLAY • FLYING HEALER • 25 HP", "0xff38bdf8", "celestial pixie medic allay flying fairy"},
                    {"Arcane Bard", "VILLAGER • WAR LUTE BUFFS • 30 HP", "0xff06b6d4", "arcane bard lute music buff"},
                    {"Combat Engineer", "VILLAGER • BARRICADE BUILDER • 32 HP", "0xff0ea5e9", "combat engineer villager builder barricade"}
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

        } else if (tab == 2) { // FACTIONS & DIPLOMACY
            drawFactionCard(context, leftX, y + 34, "Kingdom of Eldoria", "Blue (#3B82F6) • 6 Units • Hostile: Raiders, Undead", 0xff3b82f6);
            drawFactionCard(context, leftX, y + 72, "Iron Raiders", "Red (#EF4444) • 3 Units • Hostile: Kingdom, Village", 0xffef4444);
            drawFactionCard(context, leftX, y + 110, "Village Alliance", "Green (#10B981) • 3 Units • Allied: Kingdom", 0xff10b981);

            int infoY = y + 152;
            context.drawTextWithShadow(textRenderer, Text.literal("FACTION PERKS & FRIENDLY FIRE DIPLOMACY").formatted(Formatting.BOLD, Formatting.GOLD), leftX, infoY, 0xffffd700);
            context.drawText(textRenderer, Text.literal("• Military Discipline (+15% DMG)  • Heavy Armor (+20% Armor)").formatted(Formatting.WHITE), leftX, infoY + 14, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• Pyrotechnics (+50% Fireworks AoE) • Holy Might (+50% Heals)").formatted(Formatting.WHITE), leftX, infoY + 26, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• Zero friendly fire damage between allied or same-faction forces.").formatted(Formatting.GREEN), leftX, infoY + 38, 0xff4ade80, false);

        } else if (tab == 3) { // BATTLE SANDBOX
            int statsX = x + 242;
            int statsY = y + 34;

            context.drawTextWithShadow(textRenderer, Text.literal("SOLO WAR SANDBOX").formatted(Formatting.BOLD, Formatting.GOLD), statsX, statsY, 0xffffd700);
            context.drawText(textRenderer, Text.literal("Side A / B: Select Factions").formatted(Formatting.AQUA), statsX, statsY + 16, 0xff38bdf8, false);
            context.drawText(textRenderer, Text.literal("5 Scale Sizes: 8v8 up to 48v48!").formatted(Formatting.GRAY), statsX, statsY + 30, 0xff94a3b8, false);
            context.drawText(textRenderer, Text.literal("Terrain-Snapped Frontlines").formatted(Formatting.GRAY), statsX, statsY + 44, 0xff94a3b8, false);

            context.fill(leftX, y + 152, x + backgroundWidth - 14, y + backgroundHeight - 10, COLOR_PANEL_MAIN);
            context.drawTextWithShadow(textRenderer, Text.literal("AUTOMATED CASUALTY TRACKING & VICTORY SYSTEM").formatted(Formatting.GREEN, Formatting.BOLD), leftX + 8, y + 158, 0xff4ade80);
            context.drawText(textRenderer, Text.literal("• Armies automatically adapt to terrain heightmaps on deployment.").formatted(Formatting.WHITE), leftX + 8, y + 172, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("• When all units of one faction fall, victory fireworks and horn sound!").formatted(Formatting.YELLOW), leftX + 8, y + 184, 0xfffde047, false);

        } else if (tab == 4) { // SETTINGS & SIMULATION
            int infoX = x + 218;
            int infoY = y + 68;
            context.drawTextWithShadow(textRenderer, Text.literal("WORLD CONFIG & AUTHORITY").formatted(Formatting.BOLD, Formatting.GOLD), infoX, infoY, 0xffffd700);
            context.drawText(textRenderer, Text.literal("• 100% Server Authoritative Architecture").formatted(Formatting.WHITE), infoX, infoY + 16, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• AI runs throttled every 10 server ticks").formatted(Formatting.GRAY), infoX, infoY + 28, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("• Spatial queries protect TPS performance").formatted(Formatting.GRAY), infoX, infoY + 40, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("• Full data persistence across restarts").formatted(Formatting.GREEN), infoX, infoY + 52, 0xff4ade80, false);
        }
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
            String formatted = actual >= 10.0 ? String.format("%.0f", actual) : String.format("%.2f", actual);
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
