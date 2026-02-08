package com.siegemachinecompat.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.BowmanEntity;
import com.siegemachinecompat.ai.SiegeMachineTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import ru.magistu.siegemachines.api.enitity.Useable;
import ru.magistu.siegemachines.entity.machine.LadderSeat;

import java.util.EnumSet;

public class RecruitOperateAnyUseableMachineGoal extends Goal {

    private final AbstractRecruitEntity recruit;
    private Useable useable;
    private LadderSeat ladderSeat;
    private float yaw;
    private float pitch;
    private double lastDistance = Double.MAX_VALUE;
    private double lastJumpDistance = Double.MAX_VALUE;
    private int stuckTicks = 0;
    private static final int STUCK_TICKS = 2;
    private static final double DIST_EPS = 0.01D;
    private Vec3 lastPos;
    private int jumpCooldownTicks = 0;
    private static final int JUMP_COOLDOWN_TICKS = 20;
    private int failedJumpCount = 0;
    private static final int MAX_FAILED_JUMPS = 2;
    private int groundDetectTicks = 0;
    private Vec3 pendingDismountPos;

    public RecruitOperateAnyUseableMachineGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (recruit.level().isClientSide() || !recruit.isPassenger()) {
            return false;
        }

        Entity vehicle = recruit.getVehicle();
        if (vehicle instanceof Useable useable && isSupported(useable)) {
            this.useable = useable;
            this.ladderSeat = null;
            this.lastDistance = Double.MAX_VALUE;
            this.stuckTicks = 0;
            this.lastPos = null;
            this.jumpCooldownTicks = 0;
            this.lastJumpDistance = Double.MAX_VALUE;
            this.failedJumpCount = 0;
            this.groundDetectTicks = 0;
            this.pendingDismountPos = null;
            return true;
        }

        if (vehicle instanceof LadderSeat seat && seat.parent != null && isSupported(seat.parent)) {
            this.useable = seat.parent;
            this.ladderSeat = seat;
            this.lastDistance = Double.MAX_VALUE;
            this.stuckTicks = 0;
            this.lastPos = null;
            this.jumpCooldownTicks = 0;
            this.lastJumpDistance = Double.MAX_VALUE;
            this.failedJumpCount = 0;
            this.groundDetectTicks = 0;
            this.pendingDismountPos = null;
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (recruit.level().isClientSide() || !recruit.isPassenger() || !isSupported(useable) || !isUseableAlive()) {
            return false;
        }

        Entity vehicle = recruit.getVehicle();
        if (ladderSeat != null) {
            return vehicle == ladderSeat;
        }

        if (useable instanceof Entity useableEntity) {
            return vehicle == useableEntity;
        }

        return vehicle instanceof Useable && vehicle == useable;
    }

    @Override
    public void tick() {
        super.tick();

        if (useable.getUsage() == Useable.UsageType.RAM) {
            tickRam();
            return;
        }

        if (useable.getUsage() == Useable.UsageType.CLIMB) {
            tickClimb();
        }
    }

    private void tickRam() {
        BlockPos targetBlock = SiegeMachineTargets.getRamTarget(recruit.getUUID());
        Vec3 targetPos = getRamTargetPos();
        if (targetPos == null) {
            return;
        }
        Vec3 vehiclePos = getUseablePos();
        if (vehiclePos == null) {
            return;
        }

        if (targetBlock != null) {
            if (vehiclePos.distanceToSqr(Vec3.atCenterOf(targetBlock)) <= 4.0D
                    || recruit.level().getBlockState(targetBlock).isAir()) {
                SiegeMachineTargets.clearRamTarget(recruit.getUUID());
                return;
            }
        }

        driveToward(targetPos, vehiclePos);

        float dx = (float) (targetPos.x - vehiclePos.x);
        float dy = (float) (targetPos.y - vehiclePos.y);
        float dz = (float) (targetPos.z - vehiclePos.z);

        yaw = (float) ((Mth.atan2(dz, dx) * 180F / Mth.PI) - 90F);
        float horizontalDist = Mth.sqrt(dx * dx + dz * dz);
        pitch = -(float) (Mth.atan2(dy, horizontalDist) * 180F / Mth.PI);

        useable.setYawDest(yaw);
        useable.setTurretRotationsDest(pitch, yaw);
        recruit.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 360, 360);

        if (shouldUseRam(vehiclePos, targetPos)) {
            useable.use(recruit);
        }
    }

    private boolean shouldUseRam(Vec3 vehiclePos, Vec3 targetPos) {
        if (vehiclePos.distanceToSqr(targetPos) > 9.0D) {
            return false;
        }

        return Math.abs(Mth.wrapDegrees(useable.getGlobalTurretYaw()) - yaw) < 15F
                && Math.abs(Mth.wrapDegrees(useable.getTurretPitch()) - pitch) < 15F;
    }

    private void tickClimb() {
        if (ladderSeat != null) {
            if (shouldDismountFromLadder()) {
                return;
            }

            recruit.zza = 1.0F;
            return;
        }

        BlockPos targetPos = SiegeMachineTargets.getLadderTarget(recruit.getUUID());
        if (targetPos == null) {
            recruit.zza = 0F;
            return;
        }

        Vec3 vehiclePos = getUseablePos();
        if (vehiclePos == null) {
            return;
        }

        if (vehiclePos.distanceToSqr(Vec3.atCenterOf(targetPos)) <= 4.0D
                || recruit.level().getBlockState(targetPos).isAir()) {
            SiegeMachineTargets.clearLadderTarget(recruit.getUUID());
            recruit.zza = 0F;
            lastDistance = Double.MAX_VALUE;
            stuckTicks = 0;
            jumpCooldownTicks = 0;
            lastJumpDistance = Double.MAX_VALUE;
            failedJumpCount = 0;
            groundDetectTicks = 0;
            pendingDismountPos = null;
            if (useable instanceof Entity useableEntity) {
                useable.setYawDest(useableEntity.getYRot());
                useableEntity.setDeltaMovement(useableEntity.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            }
            return;
        }

        Vec3 target = Vec3.atCenterOf(targetPos);
        driveToward(target, vehiclePos);
    }

    private boolean isSupported(Useable useable) {
        if (useable == null) {
            return false;
        }

        Useable.UsageType usage = useable.getUsage();
        return usage == Useable.UsageType.RAM || usage == Useable.UsageType.CLIMB;
    }

    private boolean isUseableAlive() {
        return !(useable instanceof Entity entity) || entity.isAlive();
    }

    private Vec3 getUseablePos() {
        if (useable instanceof Entity entity) {
            return entity.position();
        }

        return null;
    }

    private Vec3 getRamTargetPos() {
        if (recruit instanceof BowmanEntity bowman && bowman.getShouldStrategicFire() && bowman.StrategicFirePos() != null) {
            return bowman.StrategicFirePos().getCenter();
        }

        BlockPos targetPos = SiegeMachineTargets.getRamTarget(recruit.getUUID());
        if (targetPos != null) {
            return targetPos.getCenter();
        }

        LivingEntity target = recruit.getTarget();
        if (target != null && target.isAlive()) {
            return target.position().add(0F, target.getBbHeight() * 0.25F, 0F);
        }

        return null;
    }

    private void driveToward(Vec3 targetPos, Vec3 vehiclePos) {
        if (!(useable instanceof Entity useableEntity)) {
            return;
        }

        recruit.getNavigation().stop();

        double distSqr = vehiclePos.distanceToSqr(targetPos);
        updateStuck(useableEntity, distSqr, targetPos);
        if (distSqr <= 4.0D) {
            useableEntity.setDeltaMovement(useableEntity.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            return;
        }

        float dx = (float) (targetPos.x - vehiclePos.x);
        float dz = (float) (targetPos.z - vehiclePos.z);

        float desiredYaw = (float) ((Mth.atan2(dz, dx) * 180F / Mth.PI) - 90F);
        useable.setYawDest(desiredYaw);
        useableEntity.setYRot(desiredYaw);
        useableEntity.setYHeadRot(desiredYaw);
        recruit.setYRot(desiredYaw);
        recruit.setYHeadRot(desiredYaw);
        recruit.yBodyRot = desiredYaw;
        recruit.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 360, 360);

        recruit.zza = 1.0F;
        recruit.xxa = 0.0F;
        recruit.yya = 0.0F;

        double speed = 0.10D;
        double yawRad = Math.toRadians(desiredYaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 current = useableEntity.getDeltaMovement();
        Vec3 step = forward.scale(speed);
        useableEntity.setDeltaMovement(step.add(0.0D, current.y, 0.0D));
        useableEntity.move(MoverType.SELF, step);
        tryStepUp(useableEntity, forward);
    }

    private boolean shouldDismountFromLadder() {
        if (ladderSeat == null) {
            return false;
        }

        return false;
    }

    private void tryStepUp(Entity useableEntity, Vec3 forward) {
        BlockPos ahead = BlockPos.containing(
                useableEntity.getX() + forward.x,
                useableEntity.getY(),
                useableEntity.getZ() + forward.z
        );
        BlockPos above = ahead.above();

        boolean blocked = useableEntity.level().getBlockState(ahead).isCollisionShapeFullBlock(useableEntity.level(), ahead);
        boolean spaceAbove = useableEntity.level().getBlockState(above).isAir();
        if (blocked && spaceAbove) {
            double targetY = ahead.getY() + 1.0D;
            useableEntity.setPos(useableEntity.getX(), targetY, useableEntity.getZ());
            useableEntity.move(MoverType.SELF, new Vec3(forward.x * 0.4D, 0.0D, forward.z * 0.4D));
        }
    }

    private void updateStuck(Entity useableEntity, double distSqr, Vec3 targetPos) {
        if (distSqr <= 4.0D) {
            stuckTicks = 0;
            lastDistance = distSqr;
            return;
        }
        if (failedJumpCount >= MAX_FAILED_JUMPS) {
            useableEntity.setDeltaMovement(useableEntity.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            return;
        }
        if (jumpCooldownTicks > 0) {
            jumpCooldownTicks--;
        }
        boolean noDistanceProgress = lastDistance - distSqr < DIST_EPS;
        boolean noForwardProgress = true;
        Vec3 currentPos = useableEntity.position();
        if (lastPos != null) {
            Vec3 move = currentPos.subtract(lastPos);
            Vec3 toTarget = targetPos.subtract(currentPos);
            if (toTarget.lengthSqr() > 0.0001D) {
                double forward = move.dot(toTarget.normalize());
                noForwardProgress = forward < 0.001D;
            }
        }
        lastPos = currentPos;

        if (noDistanceProgress || noForwardProgress) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        lastDistance = distSqr;

        if (stuckTicks >= STUCK_TICKS && jumpCooldownTicks == 0) {
            if (lastJumpDistance - distSqr < DIST_EPS) {
                failedJumpCount++;
            } else {
                failedJumpCount = 0;
            }
            lastJumpDistance = distSqr;
            useableEntity.move(MoverType.SELF, new Vec3(0.0D, 2.0D, 0.0D));
            stuckTicks = 0;
            jumpCooldownTicks = JUMP_COOLDOWN_TICKS;
        }
    }
}
