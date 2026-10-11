package dev.qynl.racers.client.screen;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import dev.qynl.racers.client.ClientRaceState;
import dev.qynl.racers.net.ModNetworking;

/**
 * The circuit catalogue shown by the track wand: pick a layout, pick a
 * rotation, build it. Also the entry point for recording your own.
 */
public class TrackSelectScreen extends Screen {
    private int page;
    private int rotation;
    private static final int PER_PAGE = 8;

    public TrackSelectScreen(Text title) {
        super(title);
        this.rotation = ClientRaceState.wandRotation;
    }

    @Override
    protected void init() {
        super.init();
        List<ClientRaceState.TrackInfo> tracks = ClientRaceState.tracks;
        int pages = Math.max(1, (tracks.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        int start = page * PER_PAGE;
        int y = 60;
        for (int i = start; i < Math.min(tracks.size(), start + PER_PAGE); i++) {
            ClientRaceState.TrackInfo info = tracks.get(i);
            int rowY = y + (i - start) * 22;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Place " + info.name()), button -> {
                        ClientRaceState.send(ModNetworking.REQ_PLACE_TRACK, 0, rotation, 0, info.id());
                        close();
                    }).dimensions(width / 2 - 150, rowY, 120, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.rotate",
                        rotation), button -> {
                    rotation = (rotation + 90) % 360;
                    clearAndInit();
                })
                .dimensions(width / 2 - 24, height - 58, 120, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.fabricracers.record"),
                        button -> {
                            ClientRaceState.send(ModNetworking.REQ_RECORD_TRACK, 0, 7, 0, "");
                            close();
                        })
                .dimensions(width / 2 - 150, height - 58, 120, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                    page = Math.max(0, page - 1);
                    clearAndInit();
                }).dimensions(width / 2 - 150, height - 34, 40, 20).build()).active = page > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                    page++;
                    clearAndInit();
                }).dimensions(width / 2 - 104, height - 34, 40, 20).build())
                .active = (page + 1) * PER_PAGE < ClientRaceState.tracks.size();
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(width / 2 + 90, height - 34, 60, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFF2C42A);
        String anchor = ClientRaceState.wandAnchor == null ? "?"
                : ClientRaceState.wandAnchor.getX() + ", " + ClientRaceState.wandAnchor.getY()
                + ", " + ClientRaceState.wandAnchor.getZ();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.fabricracers.anchor", anchor, rotation), width / 2, 28,
                0xFFAAAAAA);
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(ClientRaceState.tracks.size(), start + PER_PAGE); i++) {
            ClientRaceState.TrackInfo info = ClientRaceState.tracks.get(i);
            int rowY = 60 + (i - start) * 22;
            context.drawTextWithShadow(textRenderer,
                    Text.literal(info.sizeX() + "x" + info.sizeZ() + "  " + info.checkpoints()
                            + " checkpoints  " + info.laps() + " laps  " + info.width() + " wide"
                            + (info.stock() ? "" : "  [player]")).formatted(
                            info.stock() ? Formatting.GRAY : Formatting.GREEN),
                    width / 2 - 20, rowY + 6, 0xFFAAAAAA);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
