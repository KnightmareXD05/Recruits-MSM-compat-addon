package com.siegemachinecompat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.siegemachinecompat.ai.SiegeLadderAssignments;
import com.siegemachinecompat.ai.SiegeMachineTargets;
import com.siegemachinecompat.command.LadderSelectionStore;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.entities.RecruitShieldmanEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.magistu.siegemachines.entity.machine.SiegeLadder;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = "siege_machine_compat")
public class LadderTargetCommand {

    private static final double COMMAND_RADIUS = 20.0D;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("laddertarget")
                .requires(source -> source.hasPermission(0))
                .executes(LadderTargetCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        BlockHitResult hit = getLookBlock(player);
        if (hit == null) {
            source.sendFailure(Component.literal("No block in sight."));
            return 0;
        }

        SiegeLadder ladder = getSelectedLadder(player);
        if (ladder == null) {
            ladder = getLookLadder(player);
        }
        if (ladder == null) {
            ladder = findLadderNearTarget(player, hit.getBlockPos());
        }
        if (ladder == null) {
            ladder = findLadderNearPlayer(player);
        }

        if (ladder == null) {
            source.sendFailure(Component.literal("No siege ladder near target or player."));
            return 0;
        }

        AbstractRecruitEntity currentDriver = null;
        if (ladder.getControllingPassenger() instanceof AbstractRecruitEntity recruit && isEligibleRecruit(player, recruit)) {
            currentDriver = recruit;
        }

        List<AbstractRecruitEntity> recruits = player.level().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ladder.getBoundingBox().inflate(COMMAND_RADIUS),
                recruit -> isEligibleRecruit(player, recruit) && !recruit.isPassenger()
        );

        AbstractRecruitEntity driver = currentDriver != null ? currentDriver : recruits.stream()
                .min(Comparator.comparingDouble(ladder::distanceToSqr))
                .orElse(null);

        if (driver == null) {
            source.sendFailure(Component.literal("No available recruits to drive the ladder."));
            return 0;
        }

        SiegeLadderAssignments.assignDriver(driver.getUUID(), ladder.getId());
        SiegeMachineTargets.setLadderTarget(driver.getUUID(), hit.getBlockPos());
        if (driver.getVehicle() != ladder) {
            driver.stopRiding();
            driver.startRiding(ladder);
        }
        source.sendSuccess(() -> Component.literal("Assigned ladder driver."), false);
        return 1;
    }

    private static boolean isEligibleRecruit(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (!(recruit instanceof RecruitEntity || recruit instanceof RecruitShieldmanEntity)) {
            return false;
        }

        if (recruit.getOwnerUUID() == null) {
            return false;
        }

        return recruit.getOwnerUUID().equals(player.getUUID());
    }

    private static SiegeLadder findLadderNearTarget(ServerPlayer player, BlockPos targetPos) {
        Vec3 target = Vec3.atCenterOf(targetPos);
        return player.level().getEntitiesOfClass(
                SiegeLadder.class,
                player.getBoundingBox().inflate(COMMAND_RADIUS),
                ladder -> ladder.isAlive() && ladder.distanceToSqr(target) <= COMMAND_RADIUS * COMMAND_RADIUS
        ).stream().min(Comparator.comparingDouble(ladder -> ladder.distanceToSqr(target))).orElse(null);
    }

    private static SiegeLadder findLadderNearPlayer(ServerPlayer player) {
        return player.level().getEntitiesOfClass(
                SiegeLadder.class,
                player.getBoundingBox().inflate(COMMAND_RADIUS),
                SiegeLadder::isAlive
        ).stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static SiegeLadder getSelectedLadder(ServerPlayer player) {
        Integer id = LadderSelectionStore.get(player.getUUID());
        if (id == null) {
            return null;
        }
        return player.level().getEntity(id) instanceof SiegeLadder ladder ? ladder : null;
    }

    private static SiegeLadder getLookLadder(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(128.0D));
        List<SiegeLadder> ladders = player.level().getEntitiesOfClass(
                SiegeLadder.class,
                player.getBoundingBox().inflate(64.0D),
                SiegeLadder::isAlive
        );
        SiegeLadder best = null;
        double bestDist = Double.MAX_VALUE;
        for (SiegeLadder ladder : ladders) {
            Vec3 hit = ladder.getBoundingBox().clip(start, end).orElse(null);
            if (hit == null) {
                continue;
            }
            double dist = start.distanceToSqr(hit);
            if (dist < bestDist) {
                bestDist = dist;
                best = ladder;
            }
        }
        return best;
    }

    private static BlockHitResult getLookBlock(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(64.0D));
        HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }
}
