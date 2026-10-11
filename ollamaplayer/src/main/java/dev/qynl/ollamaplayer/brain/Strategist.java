package dev.qynl.ollamaplayer.brain;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Event-driven strategic decisions. Asked only when something changes (hurt, enemy appears, and so on),
 * never on a timer. At most one request is in flight, and requests are spaced by a cooldown. Failures,
 * malformed output and timeouts all end in {@link Decision#SAFE}, so nothing ever blocks on the model.
 */
public final class Strategist {
    public enum Outcome { SENT, SKIPPED_IN_FLIGHT, SKIPPED_COOLDOWN }

    /** Model port: takes chat messages, returns the assistant text. Injectable for tests. */
    public interface Port extends Function<List<JsonObject>, CompletableFuture<String>> {}

    private final Port port;
    private final long cooldownNanos;
    private final Metrics metrics;
    private volatile boolean inFlight;
    private long lastSentNanos = Long.MIN_VALUE / 2;
    private volatile Decision latest;

    public Strategist(Port port, long cooldownNanos, Metrics metrics) {
        this.port = port;
        this.cooldownNanos = cooldownNanos;
        this.metrics = metrics;
    }

    /** Asks the model about one event, unless a request is already out or the cooldown has not passed. */
    public Outcome consider(List<JsonObject> messages, long now) {
        if (inFlight) return Outcome.SKIPPED_IN_FLIGHT;
        if (now - lastSentNanos < cooldownNanos) return Outcome.SKIPPED_COOLDOWN;
        inFlight = true;
        lastSentNanos = now;
        metrics.llmStarted(now);
        CompletableFuture<String> f;
        try {
            f = port.apply(messages);
        } catch (RuntimeException e) {
            f = CompletableFuture.failedFuture(e);
        }
        f.whenComplete((content, err) -> {
            inFlight = false;
            metrics.llmFinished(System.nanoTime() - now, err != null);
            latest = err != null ? Decision.SAFE : Decision.parse(content);
        });
        return Outcome.SENT;
    }

    /** The newest decision, handed out once so it is applied at most once. */
    public Decision takeLatest() {
        Decision d = latest;
        latest = null;
        return d;
    }

    public boolean inFlight() {
        return inFlight;
    }
}
