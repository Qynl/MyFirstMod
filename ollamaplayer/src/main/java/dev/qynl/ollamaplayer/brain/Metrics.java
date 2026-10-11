package dev.qynl.ollamaplayer.brain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Cost counters for Nova, so tuning is based on measurements. LLM completions arrive on a
 * background thread, so the LLM side is synchronized; tick and path timings come from the server thread.
 */
public final class Metrics {
    /** The one set of counters Nova uses. Written from the server thread and the LLM thread. */
    public static final Metrics SHARED = new Metrics();

    private static final long WINDOW_NANOS = 60_000_000_000L;

    private final Deque<Long> llmStarts = new ArrayDeque<>();
    private long llmTotal;
    private long llmFailures;
    private double llmLatencyEmaMs = -1;

    private double tickEmaMs = -1;
    private double tickMaxMs;
    private double pathEmaMs = -1;

    public synchronized void llmStarted(long now) {
        llmTotal++;
        llmStarts.addLast(now);
        prune(now);
    }

    public synchronized void llmFinished(long durationNanos, boolean failed) {
        if (failed) llmFailures++;
        double ms = durationNanos / 1e6;
        llmLatencyEmaMs = llmLatencyEmaMs < 0 ? ms : llmLatencyEmaMs * 0.8 + ms * 0.2;
    }

    /** Server thread: time spent in one companion tick. */
    public synchronized void tick(long durationNanos) {
        double ms = durationNanos / 1e6;
        tickEmaMs = tickEmaMs < 0 ? ms : tickEmaMs * 0.95 + ms * 0.05;
        tickMaxMs = Math.max(tickMaxMs * 0.999, ms);
    }

    /** Server thread: time spent in one path search. */
    public synchronized void path(long durationNanos) {
        double ms = durationNanos / 1e6;
        pathEmaMs = pathEmaMs < 0 ? ms : pathEmaMs * 0.9 + ms * 0.1;
    }

    private void prune(long now) {
        while (!llmStarts.isEmpty() && now - llmStarts.peekFirst() > WINDOW_NANOS) llmStarts.pollFirst();
    }

    /** One line for /nova status. */
    public synchronized String summary(long now) {
        prune(now);
        return String.format(Locale.ROOT,
                "[Cost: LLM %d/min, %d total, %d failed, avg %.1fs; tick %.2f ms avg, %.2f ms max; path %.2f ms avg]",
                llmStarts.size(), llmTotal, llmFailures, llmLatencyEmaMs < 0 ? 0 : llmLatencyEmaMs / 1000.0,
                tickEmaMs < 0 ? 0 : tickEmaMs, tickMaxMs, pathEmaMs < 0 ? 0 : pathEmaMs);
    }

    public synchronized int llmPerMinute(long now) {
        prune(now);
        return llmStarts.size();
    }
}
