package dev.qynl.myfirstmod.kinetics.client;

import dev.qynl.myfirstmod.kinetics.KineticsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client receivers for Null Kinetics server → client packets. */
public final class KineticsClientReceivers {
    private KineticsClientReceivers() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                KineticsNetworking.KineticsStatePayload.ID,
                (payload, context) -> context.client().execute(() ->
                        KineticsClientState.apply(payload)));
    }
}
