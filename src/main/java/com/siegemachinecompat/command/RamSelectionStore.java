package com.siegemachinecompat.command;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RamSelectionStore {

    private static final Map<UUID, Integer> SELECTIONS = new ConcurrentHashMap<>();

    private RamSelectionStore() {
    }

    public static void select(UUID playerId, int ramId) {
        if (playerId != null) {
            SELECTIONS.put(playerId, ramId);
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
