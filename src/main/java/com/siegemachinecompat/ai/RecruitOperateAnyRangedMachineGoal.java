package com.siegemachinecompat.ai;

import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import ru.magistu.siegemachines.entity.machine.ShootingMachine;

import java.util.EnumSet;

public class RecruitOperateAnyRangedMachineGoal extends Goal {

    private final BowmanEntity bowman;
    private ShootingMachine machine;

    public RecruitOperateAnyRangedMachineGoal(BowmanEntity bowman) {
        this.bowman = bowman;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!bowman.isPassenger())
            return false;

        if (!(bowman.getVehicle() instanceof ShootingMachine sm))
            return false;

        machine = sm;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return bowman.isPassenger()
                && bowman.getVehicle() == machine
                && machine.isAlive();
    }

    @Override
    public void tick() {

        LivingEntity target = bowman.getTarget();
        if (target == null || !target.isAlive())
            return;

        Vec3 eye = bowman.getEyePosition();
        Vec3 aim = target.getEyePosition();

        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;

        double flat = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float)(Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;

        bowman.setYRot(yaw);
        bowman.yHeadRot = yaw;
        bowman.yBodyRot = yaw;

        float directPitch = (float)(-(Mth.atan2(dy, flat) * (180F / Math.PI)));

        float v = machine.getProjectileInitSpeed();
        float g = 0.03F;

        double v2 = v * v;

        double disc = v2 * v2 - g * (g * flat * flat + 2 * dy * v2);

        float finalPitch;

        if (disc < 0) {
            finalPitch = directPitch;
        } else {
            double sqrt = Math.sqrt(disc);

            double tanHigh = (v2 + sqrt) / (g * flat);

            float ballisticPitch = (float)(-Math.toDegrees(Math.atan(tanHigh)));

            finalPitch = ballisticPitch;
        }

        double elev = dy;
        double distance = Math.max(1.0, flat);

        double heightCorrection = (elev / distance) * 25.0;
        heightCorrection = Mth.clamp(heightCorrection, -25.0, 25.0);

        double distBlocks = flat;

        double rangeCorrection =
                0.0035 * distBlocks * distBlocks;

        rangeCorrection = Mth.clamp(rangeCorrection, 0.0, 35.0);

        finalPitch += (float) heightCorrection;
        finalPitch -= (float) rangeCorrection;

        bowman.setXRot(finalPitch);
        bowman.xRotO = finalPitch;

        bowman.yRotO = yaw;

        bowman.getLookControl().setLookAt(
                target.getX(),
                target.getEyeY(),
                target.getZ(),
                360,
                360
        );
    }
}
