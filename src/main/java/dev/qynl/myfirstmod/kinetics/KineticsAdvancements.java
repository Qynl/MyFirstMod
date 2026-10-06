package dev.qynl.myfirstmod.kinetics;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Awards the Null Kinetics advancement tree from code.
 *
 * <p>Every advancement JSON uses the {@code minecraft:impossible} trigger and a
 * criterion named {@code unlocked}; granting it from here is the classic, safe
 * way to unlock data-driven advancements for custom behaviour.
 */
public final class KineticsAdvancements {
    private KineticsAdvancements() {}

    /** Unlocks the tree root — call on the first ability use of any kind. */
    public static void grantRoot(ServerPlayerEntity player) {
        grant(player, "root");
    }

    public static void grant(ServerPlayerEntity player, String name) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        Identifier id = Identifier.of(MyFirstMod.MOD_ID, "kinetics/" + name);
        AdvancementEntry entry = server.getAdvancementLoader().get(id);
        if (entry == null) return;
        player.getAdvancementTracker().grantCriterion(entry, "unlocked");
    }
}
