package com.siegemachinecompat.ai;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SiegeLadderAssignments {

    public enum Role {
        DRIVER,
        PASSENGER
    }

    private static final Map<UUID, Integer> ASSIGNMENTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Role> ROLES = new ConcurrentHashMap<>();

    private SiegeLadderAssignments() {
    }

    public static void assignDriver(UUID recruitId, int ladderEntityId) {
        if (recruitId != null) {
            ASSIGNMENTS.put(recruitId, ladderEntityId);
            ROLES.put(recruitId, Role.DRIVER);
        }
    }

    public static void assignPassenger(UUID recruitId, int ladderEntityId) {
        if (recruitId != null) {
            ASSIGNMENTS.put(recruitId, ladderEntityId);
            ROLES.put(recruitId, Role.PASSENGER);
        }
    }

    public static Integer getLadder(UUID recruitId) {
        return recruitId == null ? null : ASSIGNMENTS.get(recruitId);
    }

    public static Role getRole(UUID recruitId) {
        return recruitId == null ? null : ROLES.get(recruitId);
    }

    public static void clear(UUID recruitId) {
        if (recruitId != null) {
            ASSIGNMENTS.remove(recruitId);
            ROLES.remove(recruitId);
        }
    }
}
