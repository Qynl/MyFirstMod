package dev.qynl.racers.client.screen;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarStats;
import dev.qynl.racers.car.CarType;
import dev.qynl.racers.car.PaintColors;
import dev.qynl.racers.car.Tune;
import dev.qynl.racers.client.ClientRaceState;
import dev.qynl.racers.net.ModNetworking;

/**
 * The garage: tune every system, repaint, hand the car to the AI, repair it.
 *
 * <p>Everything is a plain client screen; the server validates ownership and
 * consumes the upgrade parts, so no inventory handler is involved.
 */
public class GarageScreen extends Screen {
    private static final int PANEL = 0xE0101014;

    public GarageScreen() {
        super(Text.translatable("screen.fabricracers.garage"));
    }

    @Override
    protected void init() {
        super.init();
        CarData data = ClientRaceState.garageData;
        if (data == null) {
            close();
            return;
        }
        int x = width / 2 - 150;
        int y = 76;
        int index = 0;
        for (Tune tune : Tune.values()) {
            int level = data.level(tune);
            int row = y + index * 22;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("+ " + tune.displayName), button -> {
                        ClientRaceState.send(ModNetworking.REQ_TUNE,
                                ClientRaceState.garageEntityId, tune.ordinal(), 0, "");
                    }).dimensions(x + 178, row, 62, 20).build()).active = level < tune.maxLevel;
            index++;
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.paint_primary"),
                button -> ClientRaceState.send(ModNetworking.REQ_PAINT,
                        ClientRaceState.garageEntityId, 0, PaintColors.next(data.primaryColor(), 1), ""))
                .dimensions(x, y + 168, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.paint_secondary"),
                button -> ClientRaceState.send(ModNetworking.REQ_PAINT,
                        ClientRaceState.garageEntityId, 1, PaintColors.next(data.secondaryColor(), 1), ""))
                .dimensions(x + 124, y + 168, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.repair"),
                        button -> ClientRaceState.send(ModNetworking.REQ_REPAIR,
                                ClientRaceState.garageEntityId, 0, 0, ""))
                .dimensions(x, y + 192, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(ClientRaceState.garageAi
                        ? Text.translatable("screen.fabricracers.ai_off")
                        : Text.translatable("screen.fabricracers.ai_on"),
                button -> ClientRaceState.send(ModNetworking.REQ_AI_TOGGLE,
                        ClientRaceState.garageEntityId, ClientRaceState.garageAi ? 0 : 1, 0, ""))
                .dimensions(x + 124, y + 192, 116, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 50, height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        CarData data = ClientRaceState.garageData;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFF2C42A);
        if (data == null) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }
        int x = width / 2 - 150;
        int y = 76;
        CarType type = data.type;
        CarStats stats = data.stats();

        context.drawCenteredTextWithShadow(textRenderer, type.label(), width / 2, 26, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.fabricracers.stats",
                        String.format("%.0f", stats.topSpeedKmh()),
                        String.format("%.1f", stats.power()),
                        String.format("%.1f", stats.grip()),
                        ClientRaceState.garageHealth + "%"), width / 2, 38, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.fabricracers.hint_parts"), width / 2, 50, 0xFF888888);

        int index = 0;
        for (Tune tune : Tune.values()) {
            int row = y + index * 22;
            int level = data.level(tune);
            context.fill(x - 4, row - 2, x + 172, row + 18, PANEL);
            context.drawTextWithShadow(textRenderer, tune.displayName, x, row + 2, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer,
                    Text.literal(level + "/" + tune.maxLevel).formatted(Formatting.GOLD),
                    x + 74, row + 2, 0xFFF2C42A);
            int barX = x + 100;
            context.fill(barX, row + 4, barX + 70, row + 14, 0xFF202028);
            int filled = (int) (70.0F * level / tune.maxLevel);
            context.fill(barX, row + 4, barX + filled, row + 14, 0xFF4FD05A);
            int cost = ClientRaceState.garageExtra.getInt(tune.id);
            int owned = ClientRaceState.garageExtra.getInt(tune.id + "_owned");
            context.drawTextWithShadow(textRenderer,
                    Text.literal(level >= tune.maxLevel ? "MAX"
                            : cost + "x (" + owned + ")"), barX - 0, row + 18 - 22 + 16, 0xFFAAAAAA);
            index++;
        }

        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.fabricracers.colors",
                        PaintColors.nameOf(data.primaryColor),
                        PaintColors.nameOf(data.secondaryColor)),
                x, y + 148, 0xFFDDDDDD);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
