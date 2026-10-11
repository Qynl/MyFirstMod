package dev.qynl.ollamaplayer.companion;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact lessons Nova has learned from bad outcomes. Not a chat log: each entry is one
 * lesson with a count of how often it was confirmed. Bounded to {@link #MAX} entries; the
 * least-confirmed lesson is dropped first.
 */
public final class Experience {
    public static final int MAX = 40;

    public static final String RETREAT_EARLY = "retreat earlier when health is low";
    public static final String STASH_FIRST = "stash before long mining trips";

    public record Lesson(String situation, String lesson, int count) {}

    private final List<Lesson> lessons = new ArrayList<>();

    /** Records that this situation happened again and taught this lesson. */
    public void record(String situation, String lesson) {
        for (int i = 0; i < lessons.size(); i++) {
            Lesson l = lessons.get(i);
            if (l.lesson().equals(lesson)) {
                lessons.set(i, new Lesson(situation, lesson, l.count() + 1));
                return;
            }
        }
        lessons.add(new Lesson(situation, lesson, 1));
        if (lessons.size() > MAX) {
            int worst = 0;
            for (int i = 1; i < lessons.size(); i++) {
                if (lessons.get(i).count() < lessons.get(worst).count()) worst = i;
            }
            lessons.remove(worst);
        }
    }

    public int count(String lesson) {
        for (Lesson l : lessons) {
            if (l.lesson().equals(lesson)) return l.count();
        }
        return 0;
    }

    public int size() {
        return lessons.size();
    }

    /** The most confirmed lessons, for the model's prompt. */
    public List<String> top(int n) {
        List<Lesson> sorted = new ArrayList<>(lessons);
        sorted.sort((a, b) -> Integer.compare(b.count(), a.count()));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, sorted.size()); i++) out.add(sorted.get(i).lesson());
        return out;
    }

    public NbtList toNbt() {
        NbtList list = new NbtList();
        for (Lesson l : lessons) {
            NbtCompound c = new NbtCompound();
            c.putString("situation", l.situation());
            c.putString("lesson", l.lesson());
            c.putInt("count", l.count());
            list.add(c);
        }
        return list;
    }

    public void load(NbtList list) {
        lessons.clear();
        for (int i = 0; i < list.size() && lessons.size() < MAX; i++) {
            NbtCompound c = list.getCompound(i);
            String lesson = c.getString("lesson");
            if (lesson.isEmpty()) continue;
            lessons.add(new Lesson(c.getString("situation"), lesson, Math.max(1, c.getInt("count"))));
        }
    }
}
