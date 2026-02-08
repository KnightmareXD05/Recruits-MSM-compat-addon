package com.siegemachinecompat.command;

import com.siegemachinecompat.ai.SiegeLadderAssignments;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.entities.RecruitShieldmanEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
public class LadderClimbCommand {

    private static final double COMMAND_RADIUS = 20.0D;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ladderclimb")
                .requires(source -> source.hasPermission(0))
                .executes(LadderClimbCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        int assigned = 0;
        BlockHitResult hit = getLookBlock(player);
        SiegeLadder ladder = hit != null ? findLadderNearTarget(player, hit.getBlockPos()) : null;
        if (ladder == null) {
            ladder = findLadderNearPlayer(player);
        }

        if (ladder == null) {
            source.sendFailure(Component.literal("No siege ladder near target or player."));
            return 0;
        }
        if (!ladder.hasControllingPassenger()) {
            source.sendFailure(Component.literal("No ladder driver. Use /laddertarget first."));
            return 0;
        }

        List<AbstractRecruitEntity> recruitsNearLadder = player.level().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ladder.getBoundingBox().inflate(COMMAND_RADIUS),
                recruit -> isEligibleRecruit(player, recruit)
        );
        List<AbstractRecruitEntity> recruitsNearPlayer = player.level().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(COMMAND_RADIUS),
                recruit -> isEligibleRecruit(player, recruit)
        );

        java.util.Set<AbstractRecruitEntity> recruits = new java.util.HashSet<>();
        recruits.addAll(recruitsNearLadder);
        recruits.addAll(recruitsNearPlayer);

        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit.isPassenger()) {
                continue;
            }
            SiegeLadderAssignments.assignPassenger(recruit.getUUID(), ladder.getId());
            assigned++;
        }

        int finalAssigned = assigned;
        source.sendSuccess(() -> Component.literal("Assigned " + finalAssigned + " recruits to climb ladders."), false);
        return assigned;
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

    private static BlockHitResult getLookBlock(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(64.0D));
        HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }
}
