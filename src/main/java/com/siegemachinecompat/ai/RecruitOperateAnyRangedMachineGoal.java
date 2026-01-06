package com.siegemachinecompat.ai;

import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ru.magistu.siegemachines.api.enitity.Shootable;
import ru.magistu.siegemachines.util.CartesianGeometry;
import java.util.EnumSet;

public class RecruitOperateAnyRangedMachineGoal extends Goal {

    private final BowmanEntity bowman;
    private Shootable shootable;
    private float yaw;
    private float pitch;
    private float errorPitch;
    private float errorYaw;
    private int errorDelayTicks = 0;
    private int reloadDelayTicks = 0;

    public RecruitOperateAnyRangedMachineGoal(BowmanEntity bowman) {
        this.bowman = bowman;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (bowman.level().isClientSide()
                || !bowman.isPassenger()
                || !(bowman.getVehicle() instanceof Shootable shootable))
            return false;

        this.shootable = shootable;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !bowman.level().isClientSide()
                && bowman.isPassenger()
                && bowman.getVehicle() == shootable
                && shootable.asLivingEntity().isAlive();
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 targetPos = getTargetPos();
        if (targetPos == null) {
            return;
        }
        if (errorDelayTicks <= 0) {
            errorPitch = bowman.level().random.nextFloat() * 5F - 4F;
            errorYaw = bowman.level().random.nextFloat() - 0.5F;
            errorDelayTicks = shootable.getMachineType().specs.delaytime.get();
        } else {
            errorDelayTicks--;
        }
        if (reloadDelayTicks <= 0) {
            reload();
            reloadDelayTicks = 60;
        } else {
            reloadDelayTicks--;
        }
        takeAim(targetPos);
        if (bowman.getShouldRanged()) {
            if (shootable.hasAmmo()) {
                fireIfAimed();
            }
        } else {
            bowman.setTarget(null);
            bowman.setStrategicFirePos(null);
            bowman.setShouldStrategicFire(false);
        }
    }

    private void reload() {
        if (shootable.hasAmmo()) {
            return;
        }
        for (int i = 0; i < bowman.getInventory().getContainerSize(); ++i) {
            ItemStack stack = bowman.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack stack1 = shootable.reload(stack);
            if (stack1 != stack) {
                bowman.getInventory().setItem(i, stack1);
                if (shootable.hasAmmo()) {
                    if (bowman.getOwner() != null) {
                        bowman.getOwner().sendSystemMessage(Component.translatable("siege_machine_compat.reloaded", bowman.getDisplayName(), shootable.asLivingEntity().getDisplayName()));
                    }
                    break;
                }
            }
        }
    }

    private void fireIfAimed() {
        if (Math.abs(Mth.wrapDegrees(shootable.getGlobalTurretYaw()) - yaw) < 1F && Math.abs(Mth.wrapDegrees(shootable.getTurretPitch()) - pitch) < 1F) {
            shootable.use(bowman);
        }
    }

    private void takeAim(Vec3 targetPos) {
        Vec3 vehiclePos = shootable.asLivingEntity().position();
        float yaw0 = calculateYaw(targetPos, vehiclePos);
        yaw = Mth.wrapDegrees(errorYaw + yaw0);
        pitch = Mth.wrapDegrees(errorPitch + calculatePitch(targetPos, vehiclePos, shootable, yaw0));
        boolean shouldRotateTurret = shootable.getMachineType().rotationspeed == 0F;
        shootable.setYawDest(shouldRotateTurret ? shootable.asLivingEntity().getYRot() : yaw);
        shootable.setTurretRotationsDest(pitch, shouldRotateTurret ? yaw : 0F);
        bowman.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 360, 360);
    }

    private @Nullable Vec3 getTargetPos() {
        LivingEntity target = bowman.getTarget();
        if (target != null && target.isAlive()) {
            Vec3 vehiclePos = shootable.asLivingEntity().position();
            Vec3 targetPos = target.position().add(0F, target.getBbHeight() * 0.25F, 0F);
            float dx = (float) (targetPos.x - vehiclePos.x);
            float dz = (float) (targetPos.z - vehiclePos.z);
            float flightTime = Mth.sqrt(dx * dx + dz * dz) / shootable.getProjectileInitSpeed();
            targetPos.add(target.getDeltaMovement().scale(flightTime));
            return targetPos;
        } else if (bowman.getShouldStrategicFire() && bowman.StrategicFirePos() != null) {
            return bowman.StrategicFirePos().getCenter();
        } else {
            return null;
        }
    }

    private float calculateYaw(Vec3 targetPos, Vec3 vehiclePos) {
        float dx = (float) (targetPos.x - vehiclePos.x);
        float dz = (float) (targetPos.z - vehiclePos.z);

        return (float) ((Mth.atan2(dz, dx) * 180F / Mth.PI) - 90F);
    }

    private float calculatePitch(Vec3 targetPos, Vec3 vehiclePos, Shootable shootable, float yaw) {
        Vec3 turretPivot = vehiclePos.add(CartesianGeometry.applyRotations(shootable.getMachineType().turretpivot, 0F, yaw));

        float dx = (float) (targetPos.x - turretPivot.x);
        float dy = (float) (targetPos.y - turretPivot.y);
        float dz = (float) (targetPos.z - turretPivot.z);

        float horizontalDist = Mth.sqrt(dx * dx + dz * dz);
        float v = shootable.getProjectileInitSpeed();
        float g = shootable.getProjectileBuilder().gravity;

        float vSqr = v * v;
        float underSqrt = vSqr * vSqr - g * (g * horizontalDist * horizontalDist + 2 * dy * vSqr);

        if (underSqrt < 0) {
            return -45.0F; // target is too far away, so we use angle for maximum fire distance
        }

        float sqrt = Mth.sqrt(underSqrt);

        return -(float) Mth.atan2(vSqr - sqrt, g * horizontalDist) * 180F / Mth.PI;
    }
}
