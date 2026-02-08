package com.siegemachinecompat.command;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LadderSelectionStore {

    private static final Map<UUID, Integer> SELECTIONS = new ConcurrentHashMap<>();

    private LadderSelectionStore() {
    }

    public static void select(UUID playerId, int ladderId) {
        if (playerId != null) {
            SELECTIONS.put(playerId, ladderId);
        }
    }

    public static Integer get(UUID playerId) {
        return playerId == null ? null : SELECTIONS.get(playerId);
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            SELECTIONS.remove(playerId);
        }
    }
}
