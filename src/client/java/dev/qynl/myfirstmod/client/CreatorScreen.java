package dev.qynl.myfirstmod.client;

import dev.qynl.myfirstmod.gui.CreatorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CreatorScreen extends HandledScreen<CreatorScreenHandler> {
    private int tab = 0;
    private TextFieldWidget searchField;

    private static final int COLOR_BG_BACKDROP = 0xcc090d16;
    private static final int COLOR_PANEL_MAIN = 0xff131c2e;
    private static final int COLOR_PANEL_INNER = 0xff0d1522;
    private static final int COLOR_CARD_BG = 0xff1a263d;
    private static final int COLOR_CARD_BORDER = 0xff2a3c5a;

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

        int topY = y + 8;
        int tabWidth = 78;

        // Navigation Tab Bar
        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 0 ? "⚔ EDITOR" : "Editor"), b -> { tab = 0; rebuildInterface(); })
                .dimensions(x + 12 + (0 * (tabWidth + 4)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 1 ? "📋 UNITS" : "Units"), b -> { tab = 1; rebuildInterface(); })
                .dimensions(x + 12 + (1 * (tabWidth + 4)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 2 ? "🛡 FACTIONS" : "Factions"), b -> { tab = 2; rebuildInterface(); })
                .dimensions(x + 12 + (2 * (tabWidth + 4)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 3 ? "💥 BATTLE" : "Battle"), b -> { tab = 3; rebuildInterface(); })
                .dimensions(x + 12 + (3 * (tabWidth + 4)), topY, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(tab == 4 ? "⚙ SETTINGS" : "Settings"), b -> { tab = 4; rebuildInterface(); })
                .dimensions(x + 12 + (4 * (tabWidth + 4)), topY, tabWidth, 20).build());

        int leftX = x + 16;
        int rightX = x + 250;

        if (tab == 0) { // UNIT EDITOR
            addDrawableChild(ButtonWidget.builder(Text.literal("⮜ Next Unit"), b -> sendButton(3))
                    .dimensions(leftX, y + 36, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("➕ Duplicate"), b -> sendButton(4))
                    .dimensions(leftX + 115, y + 36, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Entity: Cycle"), b -> sendButton(50))
                    .dimensions(leftX, y + 56, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Role: Cycle"), b -> sendButton(51))
                    .dimensions(leftX + 115, y + 56, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Faction: Cycle"), b -> sendButton(52))
                    .dimensions(leftX, y + 76, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Rank: Cycle"), b -> sendButton(53))
                    .dimensions(leftX + 115, y + 76, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Mount: Cycle"), b -> sendButton(55))
                    .dimensions(leftX, y + 96, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Aura: Cycle"), b -> sendButton(56))
                    .dimensions(leftX + 115, y + 96, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Death: Cycle"), b -> sendButton(57))
                    .dimensions(leftX, y + 116, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Scale: Cycle"), b -> sendButton(58))
                    .dimensions(leftX + 115, y + 116, 110, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Commander: Toggle"), b -> sendButton(54))
                    .dimensions(leftX, y + 136, 225, 18).build());

            // Actions on right
            addDrawableChild(ButtonWidget.builder(Text.literal("💾 Save Slots"), b -> sendButton(0))
                    .dimensions(rightX, y + 36, 160, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ Spawn Unit (1)"), b -> sendButton(1))
                    .dimensions(rightX, y + 56, 160, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🛡 Spawn Squad (5)"), b -> sendButton(2))
                    .dimensions(rightX, y + 76, 160, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 Delete Unit"), b -> sendButton(5))
                    .dimensions(rightX, y + 96, 160, 18).build());

        } else if (tab == 1) { // SAVED UNITS LIBRARY
            searchField = new TextFieldWidget(textRenderer, leftX, y + 36, 220, 20, Text.literal("Search"));
            searchField.setPlaceholder(Text.literal("Search saved units (name/role/entity)..."));
            addDrawableChild(searchField);

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ Spawn Equipped"), b -> sendButton(1))
                    .dimensions(rightX, y + 36, 160, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🛡 Spawn Squad (5)"), b -> sendButton(2))
                    .dimensions(rightX, y + 60, 160, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Cycle Unit"), b -> sendButton(3))
                    .dimensions(rightX, y + 84, 160, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("➕ Duplicate"), b -> sendButton(4))
                    .dimensions(rightX, y + 108, 160, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 Delete"), b -> sendButton(5))
                    .dimensions(rightX, y + 132, 160, 20).build());

        } else if (tab == 2) { // FACTIONS
            addDrawableChild(ButtonWidget.builder(Text.literal("Kingdom ↔ Raiders: Toggle"), b -> sendButton(70))
                    .dimensions(rightX, y + 36, 160, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Kingdom ↔ Villagers: Toggle"), b -> sendButton(71))
                    .dimensions(rightX, y + 60, 160, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Raiders ↔ Villagers: Toggle"), b -> sendButton(72))
                    .dimensions(rightX, y + 84, 160, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Refresh Presets"), b -> sendButton(30))
                    .dimensions(rightX, y + 115, 160, 20).build());

        } else if (tab == 3) { // BATTLE SANDBOX
            addDrawableChild(ButtonWidget.builder(Text.literal("Faction A: Cycle"), b -> sendButton(65))
                    .dimensions(leftX, y + 36, 102, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Faction B: Cycle"), b -> sendButton(66))
                    .dimensions(leftX + 106, y + 36, 102, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ START BATTLE (8 vs 8) ⚔"), b -> sendButton(10))
                    .dimensions(leftX, y + 58, 210, 22).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ LARGE BATTLE (16 vs 16) ⚔"), b -> sendButton(20))
                    .dimensions(leftX, y + 82, 210, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚔ MASSIVE BATTLE (24 vs 24) ⚔"), b -> sendButton(21))
                    .dimensions(leftX, y + 104, 210, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🗑 CLEAR ALL BATTLE MOBS"), b -> sendButton(11))
                    .dimensions(leftX, y + 126, 210, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Reset Battle Stats"), b -> sendButton(12))
                    .dimensions(rightX, y + 126, 160, 20).build());

        } else if (tab == 4) { // SETTINGS
            addDrawableChild(ButtonWidget.builder(Text.literal("Building AI: " + (buildingEnabled ? "ON" : "OFF")), b -> {
                buildingEnabled = !buildingEnabled;
                sendButton(31);
                rebuildInterface();
            }).dimensions(leftX, y + 36, 185, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Potion Throwing: " + (potionsEnabled ? "ON" : "OFF")), b -> {
                potionsEnabled = !potionsEnabled;
                sendButton(32);
                rebuildInterface();
            }).dimensions(leftX, y + 60, 185, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Fireworks Artillery: " + (fireworksEnabled ? "ON" : "OFF")), b -> {
                fireworksEnabled = !fireworksEnabled;
                sendButton(33);
                rebuildInterface();
            }).dimensions(leftX, y + 84, 185, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Shield Defense: " + (shieldEnabled ? "ON" : "OFF")), b -> {
                shieldEnabled = !shieldEnabled;
                sendButton(34);
                rebuildInterface();
            }).dimensions(leftX, y + 108, 185, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Friendly Fire: " + (friendlyFireAllowed ? "ALLOWED" : "PREVENTED")), b -> {
                friendlyFireAllowed = !friendlyFireAllowed;
                sendButton(35);
                rebuildInterface();
            }).dimensions(leftX, y + 132, 185, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("🔄 Restore Default Presets"), b -> sendButton(30))
                    .dimensions(rightX, y + 36, 160, 22).build());
        }
    }

    private void sendButton(int id) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, id);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Fullscreen backdrop dimming
        context.fill(0, 0, width, height, COLOR_BG_BACKDROP);

        // Main Dialog Panel
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, COLOR_PANEL_MAIN);
        context.fill(x + 10, y + 30, x + backgroundWidth - 10, y + backgroundHeight - 10, COLOR_PANEL_INNER);

        // Header separator line
        context.fill(x + 10, y + 30, x + backgroundWidth - 10, y + 31, COLOR_CARD_BORDER);
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
        context.drawText(textRenderer, Text.literal("Unit Equipment & Inventory").formatted(Formatting.AQUA), eqX, y + 115, 0xff9ecbff, false);
        context.drawText(textRenderer, Text.literal("Player Inventory").formatted(Formatting.GRAY), x + 20, y + 148, 0xff8294ad, false);
    }

    private void drawTabContent(DrawContext context, int mouseX, int mouseY) {
        int leftX = x + 16;
        int leftY = y + 160;

        if (tab == 0) { // UNIT EDITOR CONTENT
            context.drawText(textRenderer, Text.literal("Scale & Mounts Supported! Active Unit Stats:").formatted(Formatting.GOLD), leftX, leftY, 0xffffd700, false);
            context.drawText(textRenderer, Text.literal("Shield Defense: Active Blocking  |  Potions: Enabled").formatted(Formatting.GREEN), leftX, leftY + 12, 0xffcbd5e1, false);

        } else if (tab == 1) { // SAVED UNITS LIBRARY
            int cardY = y + 62;
            String q = searchField == null ? "" : searchField.getText().toLowerCase();

            if (matches(q, "royal knight villager melee tank")) {
                drawUnitCard(context, leftX, cardY, "Royal Knight", "VILLAGER • MELEE TANK • 40 HP • 7.5 DMG", 0xff3b82f6);
                cardY += 34;
            }
            if (matches(q, "royal archer skeleton ranged")) {
                drawUnitCard(context, leftX, cardY, "Royal Archer", "SKELETON • RANGED • 24 HP • 5.0 DMG", 0xffeab308);
                cardY += 34;
            }
            if (matches(q, "field medic villager healer support potion")) {
                drawUnitCard(context, leftX, cardY, "Field Medic", "VILLAGER • HEALER/MEDIC • 28 HP • SPLASH POTIONS", 0xff10b981);
                cardY += 34;
            }
            if (matches(q, "royal pyro pillager fireworks artillery explosive")) {
                drawUnitCard(context, leftX, cardY, "Royal Pyrotechnic", "PILLAGER • FIREWORKS ARTILLERY • 30 HP", 0xffef4444);
                cardY += 34;
            }
            if (matches(q, "undead necromancer evoker summoner")) {
                drawUnitCard(context, leftX, cardY, "Undead Necromancer", "EVOKER • DARK NECROMANCER • 45 HP", 0xff8b5cf6);
                cardY += 34;
            }
            if (matches(q, "combat engineer villager barricade builder")) {
                drawUnitCard(context, leftX, cardY, "Combat Engineer", "VILLAGER • BARRICADE BUILDER • 32 HP", 0xff06b6d4);
                cardY += 34;
            }

        } else if (tab == 2) { // FACTIONS
            drawFactionCard(context, leftX, y + 36, "Kingdom of Eldoria", "Blue (#3B82F6) • 6 Units • Hostile: Raiders, Undead", 0xff3b82f6);
            drawFactionCard(context, leftX, y + 74, "Iron Raiders", "Red (#EF4444) • 3 Units • Hostile: Kingdom, Village", 0xffef4444);
            drawFactionCard(context, leftX, y + 112, "Village Alliance", "Green (#10B981) • 3 Units • Allied: Kingdom", 0xff10b981);

            int infoY = y + 155;
            context.drawTextWithShadow(textRenderer, Text.literal("FACTION PERKS & FRIENDLY FIRE RULES").formatted(Formatting.BOLD, Formatting.GOLD), leftX, infoY, 0xffffd700);
            context.drawText(textRenderer, Text.literal("• Military Discipline (+15% DMG)  • Heavy Armor (+20% Armor)").formatted(Formatting.WHITE), leftX, infoY + 14, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• Pyrotechnics (+50% Fireworks AoE) • Holy Might (+50% Heals)").formatted(Formatting.WHITE), leftX, infoY + 26, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• Friendly Fire: Zero damage between members of same/allied factions.").formatted(Formatting.GREEN), leftX, infoY + 38, 0xff4ade80, false);

        } else if (tab == 3) { // BATTLE SANDBOX
            int statsX = x + 235;
            int statsY = y + 36;

            context.drawTextWithShadow(textRenderer, Text.literal("LIVE BATTLE SANDBOX").formatted(Formatting.BOLD, Formatting.GOLD), statsX, statsY, 0xffffd700);
            context.drawText(textRenderer, Text.literal("Side A / B: Select Any Two Factions").formatted(Formatting.AQUA), statsX, statsY + 16, 0xff38bdf8, false);
            context.drawText(textRenderer, Text.literal("Formation: Frontline, Ranged, Medics, Pyro").formatted(Formatting.GRAY), statsX, statsY + 30, 0xff94a3b8, false);
            context.drawText(textRenderer, Text.literal("Tactical AI: Shield Block, Potions & Artillery").formatted(Formatting.GRAY), statsX, statsY + 44, 0xff94a3b8, false);

            context.fill(leftX, y + 150, x + backgroundWidth - 16, y + backgroundHeight - 16, COLOR_PANEL_MAIN);
            context.drawTextWithShadow(textRenderer, Text.literal("REAL-TIME BATTLE SIMULATION ACTIVE").formatted(Formatting.GREEN), leftX + 8, y + 156, 0xff4ade80);
            context.drawText(textRenderer, Text.literal("Cycle factions, choose army size, and deploy structured armies instantly!").formatted(Formatting.GRAY), leftX + 8, y + 170, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("Healers cast divine rays, Pyrotechnics launch fireworks, and Bards play songs!").formatted(Formatting.YELLOW), leftX + 8, y + 184, 0xfffde047, false);

        } else if (tab == 4) { // SETTINGS
            int infoX = x + 215;
            int infoY = y + 70;
            context.drawTextWithShadow(textRenderer, Text.literal("WORLD SIMULATION CONFIG").formatted(Formatting.BOLD, Formatting.GOLD), infoX, infoY, 0xffffd700);
            context.drawText(textRenderer, Text.literal("• Server-Authoritative Architecture").formatted(Formatting.WHITE), infoX, infoY + 16, 0xffe2e8f0, false);
            context.drawText(textRenderer, Text.literal("• AI runs throttled every 10 server ticks").formatted(Formatting.GRAY), infoX, infoY + 28, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("• Spatial queries protect TPS performance").formatted(Formatting.GRAY), infoX, infoY + 40, 0xffcbd5e1, false);
            context.drawText(textRenderer, Text.literal("• All units & factions persist across restarts").formatted(Formatting.GREEN), infoX, infoY + 52, 0xff4ade80, false);
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
        context.fill(cx, cy, cx + 220, cy + 30, COLOR_CARD_BG);
        context.fill(cx, cy, cx + 3, cy + 30, accentColor);
        context.drawText(textRenderer, Text.literal(name).formatted(Formatting.BOLD), cx + 8, cy + 4, 0xfff8fafc, false);
        context.drawText(textRenderer, Text.literal(meta), cx + 8, cy + 16, 0xff94a3b8, false);
    }

    private void drawFactionCard(DrawContext context, int cx, int cy, String name, String meta, int accentColor) {
        context.fill(cx, cy, cx + 220, cy + 34, COLOR_CARD_BG);
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
}
