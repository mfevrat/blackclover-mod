package com.blackclovermod.util;

import com.blackclovermod.BlackCloverMod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Petit planificateur côté serveur : permet de faire des effets qui durent plusieurs ticks
 * (tornades, trous noirs, ondes de choc...). Appelé à chaque tick serveur.
 */
public final class TaskScheduler {
    private static final class Task {
        int delay;
        final int duration;
        int t;
        final IntConsumer action;

        Task(int delay, int duration, IntConsumer action) {
            this.delay = delay;
            this.duration = duration;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    /** Exécute action(t) pendant "duration" ticks, t allant de 0 à duration-1. */
    public static void repeat(int duration, IntConsumer action) {
        PENDING.add(new Task(0, duration, action));
    }

    /** Exécute action une seule fois après "delay" ticks. */
    public static void later(int delay, Runnable action) {
        PENDING.add(new Task(delay, 1, t -> action.run()));
    }

    public static void tick() {
        if (!PENDING.isEmpty()) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.delay > 0) {
                task.delay--;
                continue;
            }
            try {
                task.action.accept(task.t);
            } catch (Exception e) {
                BlackCloverMod.LOGGER.error("Erreur dans une tache magique", e);
                it.remove();
                continue;
            }
            task.t++;
            if (task.t >= task.duration) it.remove();
        }
    }

    public static void clear() {
        TASKS.clear();
        PENDING.clear();
    }

    private TaskScheduler() {}
}
