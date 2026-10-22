package com.blackclovermod.magic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Mémorise quels joueurs ont un grimoire qui flotte à côté d'eux (après avoir lancé un sort). */
public final class ClientFxState {
    public record Active(int color, long start, long end) {
        /** 0 = début, 1 = fin. */
        public float progress() {
            long now = System.currentTimeMillis();
            return Math.min(1f, Math.max(0f, (now - start) / (float) Math.max(1, end - start)));
        }
    }

    private static final Map<Integer, Active> ACTIVE = new ConcurrentHashMap<>();

    public static void activate(int entityId, int color, int ticks) {
        long now = System.currentTimeMillis();
        Active old = ACTIVE.get(entityId);
        long start = (old != null && now < old.end) ? old.start : now; // garde le livre ouvert s'il est déjà là
        ACTIVE.put(entityId, new Active(color, start, now + ticks * 50L));
    }

    public static Active get(int entityId) {
        Active a = ACTIVE.get(entityId);
        if (a != null && System.currentTimeMillis() > a.end) {
            ACTIVE.remove(entityId);
            return null;
        }
        return a;
    }

    private ClientFxState() {}
}
