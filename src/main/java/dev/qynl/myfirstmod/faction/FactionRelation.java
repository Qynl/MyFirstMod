package dev.qynl.myfirstmod.faction;

import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public enum FactionRelation {
    ALLIED("Allied", Formatting.GREEN, 0x22C55E),
    NEUTRAL("Neutral", Formatting.GRAY, 0x94A3B8),
    HOSTILE("Hostile", Formatting.RED, 0xEF4444);

    private final String displayName;
    private final Formatting formatting;
    private final int colorRgb;

    FactionRelation(String displayName, Formatting formatting, int colorRgb) {
        this.displayName = displayName;
        this.formatting = formatting;
        this.colorRgb = colorRgb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Formatting getFormatting() {
        return formatting;
    }

    public int getColorRgb() {
        return colorRgb;
    }

    public boolean isHostile() {
        return this == HOSTILE;
    }

    public boolean isAllied() {
        return this == ALLIED;
    }

    public boolean isNeutral() {
        return this == NEUTRAL;
    }

    public FactionRelation next() {
        return switch (this) {
            case NEUTRAL -> HOSTILE;
            case HOSTILE -> ALLIED;
            case ALLIED -> NEUTRAL;
        };
    }
}
