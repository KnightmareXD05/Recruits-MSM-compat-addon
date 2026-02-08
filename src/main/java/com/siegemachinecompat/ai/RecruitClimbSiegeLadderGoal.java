package com.siegemachinecompat.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.siegemachinecompat.ai.SiegeMachineTargets;
import net.minecraft.world.entity.ai.goal.Goal;
import ru.magistu.siegemachines.entity.machine.LadderSeat;
import ru.magistu.siegemachines.entity.machine.SiegeLadder;

import java.util.EnumSet;

public class RecruitClimbSiegeLadderGoal extends Goal {

    private static final double MAX_DISTANCE_SQR = 400.0D;
    private static final double MOUNT_DISTANCE_SQR = 16.0D;

    private final AbstractRecruitEntity recruit;
    private SiegeLadder ladder;

    public RecruitClimbSiegeLadderGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (recruit.level().isClientSide() || recruit.isPassenger()) {
            return false;
        }

        Integer ladderId = SiegeLadderAssignments.getLadder(recruit.getUUID());
        if (ladderId == null) {
            return false;
        }

        if (!(recruit.level().getEntity(ladderId) instanceof SiegeLadder target) || !target.isAlive()) {
            SiegeLadderAssignments.clear(recruit.getUUID());
            SiegeMachineTargets.clearLadderTarget(recruit.getUUID());
            return false;
        }

        if (recruit.distanceToSqr(target) > MAX_DISTANCE_SQR) {
            return false;
        }

        if (SiegeLadderAssignments.getRole(recruit.getUUID()) == SiegeLadderAssignments.Role.PASSENGER
                && !target.hasControllingPassenger()) {
            return false;
        }

        this.ladder = target;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !recruit.level().isClientSide()
                && !recruit.isPassenger()
                && ladder != null
                && ladder.isAlive()
                && recruit.distanceToSqr(ladder) <= MAX_DISTANCE_SQR
                && SiegeLadderAssignments.getLadder(recruit.getUUID()) != null;
    }

    @Override
    public void stop() {
        this.ladder = null;
        super.stop();
    }

    @Override
    public void tick() {
        if (ladder == null) {
            return;
        }

        recruit.setTarget(null);
        recruit.getLookControl().setLookAt(ladder, 30.0F, 30.0F);
        recruit.getNavigation().moveTo(ladder, 1.1D);

        if (recruit.distanceToSqr(ladder) <= MOUNT_DISTANCE_SQR) {
            if (SiegeLadderAssignments.getRole(recruit.getUUID()) == SiegeLadderAssignments.Role.DRIVER) {
                if (!ladder.hasControllingPassenger()) {
                    recruit.startRiding(ladder);
                }
            } else if (ladder.hasControllingPassenger()) {
                LadderSeat seat = findFreeSeat();
                if (seat != null) {
                    recruit.startRiding(seat);
                }
            }
        }

        if (recruit.isPassenger()) {
            SiegeLadderAssignments.clear(recruit.getUUID());
            SiegeMachineTargets.clearLadderTarget(recruit.getUUID());
        }
    }

    private LadderSeat findFreeSeat() {
        return recruit.level().getEntitiesOfClass(
                LadderSeat.class,
                ladder.getBoundingBox().inflate(12.0D),
                seat -> seat.parent == ladder && !seat.isVehicle()
        ).stream().findFirst().orElse(null);
    }
}
