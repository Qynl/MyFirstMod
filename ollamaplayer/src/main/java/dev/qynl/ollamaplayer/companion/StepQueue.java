package dev.qynl.ollamaplayer.companion;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Runs a short list of steps from a model decision, one at a time. A step may start something that
 * takes many ticks; the queue waits until the body is no longer busy, then verifies the step actually
 * succeeded. A failed step, or a step that verifies false, clears the rest of the queue so the caller
 * can replan instead of carrying on blindly.
 */
public final class StepQueue {
    public enum Result { DONE, RUNNING, FAILED }

    public interface Executor {
        /** Starts the step. DONE or FAILED means it is already finished; RUNNING means it is underway. */
        Result start(String step);

        /** Checks, after the body is free again, that the step achieved its goal. */
        boolean succeeded(String step);
    }

    private final Deque<String> steps = new ArrayDeque<>();
    private String current;

    public void set(List<String> list) {
        steps.clear();
        steps.addAll(list);
        current = null;
    }

    public boolean active() {
        return current != null || !steps.isEmpty();
    }

    /** One tick. {@code busy} means the body is still doing an earlier task. */
    public Result tick(Executor ex, boolean busy) {
        if (current != null) {
            if (busy) return Result.RUNNING;
            String finished = current;
            current = null;
            if (!ex.succeeded(finished)) {
                steps.clear();
                return Result.FAILED;
            }
        }
        if (steps.isEmpty()) return Result.DONE;
        current = steps.poll();
        Result r = ex.start(current);
        if (r == Result.FAILED) {
            current = null;
            steps.clear();
            return Result.FAILED;
        }
        if (r == Result.DONE) current = null;
        return steps.isEmpty() && current == null ? Result.DONE : Result.RUNNING;
    }
}
