package dev.qynl.racers.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import dev.qynl.racers.client.ClientRaceState;
import dev.qynl.racers.net.ModNetworking;
import dev.qynl.racers.util.Stopwatch;

/**
 * The racing overlay: speed, gears, boost, drift meter, lap timing, live
 * standings, countdown lights and the results banner.
 */
public final class RaceHud {
    private static final int PANEL = 0xB0101014;
    private static final int ACCENT = 0xFFF2C42A;
    private static final int GREEN = 0xFF4FD05A;
    private static final int RED = 0xFFDC3B36;
    private static final int CYAN = 0xFF3CC8DC;

    private RaceHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || !ClientRaceState.hudEnabled) {
            return;
        }
        ModNetworking.Hud hud = ClientRaceState.hud;
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        TextRenderer font = client.textRenderer;

        if (hud != null && ClientRaceState.hudFresh()) {
            if (hud.state() >= 0) {
                racePanel(context, font, hud, width);
                standings(context, font, hud, width);
                if (hud.state() == 0 && hud.countdown() > 0) {
                    countdown(context, font, hud, width, height);
                }
            }
            bottomCluster(context, font, hud, width, height);
            if (!hud.message().isEmpty()) {
                banner(context, font, hud.message(), width, height);
            }
        }

        // stopwatch (client side timing)
        long elapsed = ClientRaceState.stopwatchElapsed();
        if (elapsed > 0) {
            String text = "STOPWATCH  " + Stopwatch.format(elapsed);
            int textWidth = font.getWidth(text);
            int x = width - textWidth - 8;
            int y = height - 60;
            context.fill(x - 4, y - 3, width - 4, y + 11, PANEL);
            context.drawText(font, text, x, y, ACCENT, true);
        }

        // toast style notifications
        if (!ClientRaceState.notifyMessage.isEmpty()
                && System.currentTimeMillis() - ClientRaceState.notifyTime < 4200L) {
            String message = ClientRaceState.notifyMessage;
            int textWidth = font.getWidth(message);
            int x = (width - textWidth) / 2;
            int y = height - 92;
            context.fill(x - 6, y - 4, x + textWidth + 6, y + 12, PANEL);
            context.drawText(font, message, x, y, 0xFFFFFFFF, true);
        }
    }

    private static void racePanel(DrawContext context, TextRenderer font, ModNetworking.Hud hud,
                                  int width) {
        String position = hud.total() > 1 ? "P" + hud.position() + "/" + hud.total() : "";
        String lap = "LAP " + hud.lap() + "/" + hud.laps();
        String time = "TIME " + Stopwatch.format(hud.timeMs());
        String lapTime = "LAP  " + Stopwatch.format(hud.lapTimeMs());
        String best = "BEST " + Stopwatch.format(hud.bestLapMs());

        int panelWidth = 168;
        context.fill(6, 6, 6 + panelWidth, 68, PANEL);
        context.drawText(font, hud.trackName(), 12, 12, ACCENT, true);
        context.drawText(font, lap, 12, 26, 0xFFFFFFFF, true);
        if (!position.isEmpty()) {
            context.drawText(font, position, 12 + font.getWidth(lap) + 10, 26, CYAN, true);
        }
        context.drawText(font, time, 12, 40, 0xFFDDDDDD, true);
        context.drawText(font, hud.bestLapMs() > 0 ? best : lapTime, 12, 52,
                hud.bestLapMs() > 0 ? GREEN : 0xFFBBBBBB, true);
    }

    private static void standings(DrawContext context, TextRenderer font, ModNetworking.Hud hud,
                                  int width) {
        if (hud.standings().isEmpty()) {
            return;
        }
        String[] lines = hud.standings().split("\n");
        int panelWidth = 150;
        int panelHeight = 8 + lines.length * 11;
        int x = width - panelWidth - 6;
        context.fill(x, 6, width - 6, 6 + panelHeight, PANEL);
        context.drawText(font, "STANDINGS", x + 6, 11, ACCENT, true);
        int y = 24;
        for (String line : lines) {
            context.drawText(font, font.trimToWidth(line, panelWidth - 12), x + 6, y, 0xFFEEEEEE, true);
            y += 11;
        }
    }

    private static void countdown(DrawContext context, TextRenderer font, ModNetworking.Hud hud,
                                  int width, int height) {
        String text = String.valueOf(Math.max(1, hud.countdown()));
        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0F, height / 2.0F - 40.0F, 0.0F);
        context.getMatrices().scale(5.0F, 5.0F, 1.0F);
        int textWidth = font.getWidth(text);
        context.drawText(font, text, -textWidth / 2, -font.fontHeight / 2,
                hud.countdown() <= 1 ? GREEN : RED, true);
        context.getMatrices().pop();
        String hint = "Hold nothing - brakes off at GO";
        context.drawText(font, hint, (width - font.getWidth(hint)) / 2, height / 2 + 26,
                0xFFDDDDDD, true);
    }

    private static void banner(DrawContext context, TextRenderer font, String message, int width,
                               int height) {
        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0F, height / 2.0F - 90.0F, 0.0F);
        context.getMatrices().scale(2.0F, 2.0F, 1.0F);
        int textWidth = font.getWidth(message);
        context.drawText(font, message, -textWidth / 2, 0, ACCENT, true);
        context.getMatrices().pop();
    }

    private static void bottomCluster(DrawContext context, TextRenderer font, ModNetworking.Hud hud,
                                      int width, int height) {
        // speed
        String speed = String.valueOf(hud.speedKmh());
        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0F, height - 58.0F, 0.0F);
        context.getMatrices().scale(2.4F, 2.4F, 1.0F);
        int speedWidth = font.getWidth(speed);
        context.drawText(font, speed, -speedWidth / 2, 0, 0xFFFFFFFF, true);
        context.getMatrices().pop();
        String unit = "km/h";
        context.drawText(font, unit, (width - font.getWidth(unit)) / 2, height - 32, 0xFFAAAAAA, true);

        // boost bar
        int barWidth = 120;
        int barX = (width - barWidth) / 2;
        int barY = height - 24;
        context.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 7, PANEL);
        int boostWidth = (int) (barWidth * clamp(hud.boost()));
        context.fill(barX, barY, barX + boostWidth, barY + 6,
                hud.flag(ModNetworking.Hud.FLAG_BOOSTING) ? CYAN : 0xFF3C7CDC);
        if (hud.driftScore() > 0) {
            String drift = "DRIFT " + hud.driftScore();
            context.drawText(font, drift, barX + barWidth + 8, barY - 2,
                    hud.flag(ModNetworking.Hud.FLAG_DRIFTING) ? ACCENT : 0xFFAAAAAA, true);
        }
        if (hud.flag(ModNetworking.Hud.FLAG_BOOSTING)) {
            String boost = "BOOST!";
            context.drawText(font, boost, barX - font.getWidth(boost) - 8, barY - 2, CYAN, true);
        }
        if (hud.flag(ModNetworking.Hud.FLAG_DAMAGED)) {
            String warning = "ENGINE DAMAGE";
            context.drawText(font, warning, (width - font.getWidth(warning)) / 2, height - 12,
                    RED, true);
        }
        if (hud.flag(ModNetworking.Hud.FLAG_FINISHED)) {
            String finished = "RACE OVER - AI is bringing the car home";
            context.drawText(font, finished, (width - font.getWidth(finished)) / 2, 76, GREEN, true);
        }
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

}
