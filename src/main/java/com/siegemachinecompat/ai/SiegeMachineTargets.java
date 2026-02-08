package com.siegemachinecompat.ai;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SiegeMachineTargets {

    private static final Map<UUID, BlockPos> RAM_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> LADDER_TARGETS = new ConcurrentHashMap<>();

    private SiegeMachineTargets() {
    }

    public static void setRamTarget(UUID recruitId, BlockPos pos) {
        if (recruitId != null && pos != null) {
            RAM_TARGETS.put(recruitId, pos);
        }
    }

    public static BlockPos getRamTarget(UUID recruitId) {
        return recruitId == null ? null : RAM_TARGETS.get(recruitId);
    }

    public static void clearRamTarget(UUID recruitId) {
        if (recruitId != null) {
            RAM_TARGETS.remove(recruitId);
        }
    }

    public static void setLadderTarget(UUID recruitId, BlockPos pos) {
        if (recruitId != null && pos != null) {
            LADDER_TARGETS.put(recruitId, pos);
        }
    }

    public static BlockPos getLadderTarget(UUID recruitId) {
        return recruitId == null ? null : LADDER_TARGETS.get(recruitId);
    }

    public static void clearLadderTarget(UUID recruitId) {
        if (recruitId != null) {
            LADDER_TARGETS.remove(recruitId);
        }
    }
}
