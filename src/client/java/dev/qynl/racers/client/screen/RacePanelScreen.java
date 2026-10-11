package dev.qynl.racers.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtList;

import dev.qynl.racers.client.ClientRaceState;
import dev.qynl.racers.net.ModNetworking;

/** Race control: start an event, join the grid, leave, look at records. */
public class RacePanelScreen extends Screen {
    private int laps = 3;
    private int ai = 5;
    private double skill = 0.72;

    public RacePanelScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        int x = width / 2 - 120;
        int y = 60;
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.fabricracers.start_race", laps, ai,
                                skillName()), button -> {
                            int packed = ((int) Math.round(skill * 100.0)) | (ai << 8);
                            ClientRaceState.send(ModNetworking.REQ_HOST_RACE, 0, laps, packed, "");
                            close();
                        })
                .dimensions(x, y + 60, 240, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.laps", laps),
                        button -> {
                            laps = laps >= 10 ? 1 : laps + 1;
                            clearAndInit();
                        })
                .dimensions(x, y, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.ai_count", ai),
                        button -> {
                            ai = ai >= 10 ? 0 : ai + 1;
                            clearAndInit();
                        })
                .dimensions(x + 124, y, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.skill", skillName()),
                        button -> {
                            skill = skill >= 0.95 ? 0.4 : skill + 0.15;
                            clearAndInit();
                        })
                .dimensions(x, y + 24, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.records"),
                        button -> ClientRaceState.send(ModNetworking.REQ_STATS, 0, 0, 0, ""))
                .dimensions(x + 124, y + 24, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.join_grid"),
                        button -> ClientRaceState.send(ModNetworking.REQ_JOIN_RACE, 0, 0, 0, ""))
                .dimensions(x, y + 88, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.leave_race"),
                        button -> ClientRaceState.send(ModNetworking.REQ_LEAVE_RACE, 0, 0, 0, ""))
                .dimensions(x + 124, y + 88, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 50, height - 30, 100, 20).build());
    }

    private String skillName() {
        if (skill >= 0.95) {
            return "Alien";
        }
        if (skill >= 0.8) {
            return "Pro";
        }
        if (skill >= 0.6) {
            return "Club";
        }
        return "Rookie";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFF2C42A);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.fabricracers.panel_hint"), width / 2, 30, 0xFFAAAAAA);

        ModNetworking.Hud hud = ClientRaceState.hud;
        if (hud != null && hud.state() >= 0) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(hud.trackName() + "  -  Lap " + hud.lap() + "/" + hud.laps()
                            + "  -  P" + hud.position() + "/" + hud.total()), width / 2, 44,
                    0xFFFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("screen.fabricracers.not_racing").formatted(Formatting.GRAY),
                    width / 2, 44, 0xFF888888);
        }

        // profile card
        int y = 152;
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.fabricracers.profile"),
                width / 2 - 120, y, 0xFFF2C42A);
        y += 12;
        String[] rows = {
                "Races: " + ClientRaceState.profile.getInt("races"),
                "Wins: " + ClientRaceState.profile.getInt("wins") + "   Podiums: "
                        + ClientRaceState.profile.getInt("podiums"),
                "Laps: " + ClientRaceState.profile.getInt("laps") + "   Overtakes: "
                        + ClientRaceState.profile.getInt("overtakes"),
                "Distance: " + String.format("%.0f", ClientRaceState.profile.getDouble("distance"))
                        + " blocks   Top speed: "
                        + String.format("%.0f", ClientRaceState.profile.getDouble("topSpeed")) + " km/h",
                "Drift points: " + ClientRaceState.profile.getInt("drift") + "   Trophies: "
                        + ClientRaceState.profile.getInt("trophies"),
                "Cars placed: " + ClientRaceState.profile.getInt("cars")
                        + "   Tracks recorded: " + ClientRaceState.profile.getInt("tracks"),
                "Title: " + ClientRaceState.profile.getString("title"),
        };
        for (String row : rows) {
            context.drawTextWithShadow(textRenderer, Text.literal(row), width / 2 - 120, y, 0xFFDDDDDD);
            y += 11;
        }
        NbtList bestLaps = ClientRaceState.profile.getList("bestLaps", 10);
        if (!bestLaps.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.fabricracers.best_laps"),
                    width / 2 + 20, 152, 0xFFF2C42A);
            int lineY = 164;
            for (int i = 0; i < Math.min(10, bestLaps.size()); i++) {
                var entry = bestLaps.getCompound(i);
                context.drawTextWithShadow(textRenderer,
                        Text.literal(entry.getString("track") + "  "
                                + dev.qynl.racers.util.Stopwatch.format(entry.getLong("ms"))),
                        width / 2 + 20, lineY, 0xFFBBBBBB);
                lineY += 11;
            }
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
