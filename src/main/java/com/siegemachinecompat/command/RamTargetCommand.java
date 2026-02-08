package com.siegemachinecompat.command;

import com.siegemachinecompat.ai.SiegeMachineTargets;
import com.siegemachinecompat.command.RamSelectionStore;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import ru.magistu.siegemachines.api.enitity.Useable;

@Mod.EventBusSubscriber(modid = "siege_machine_compat")
public class RamTargetCommand {

    private static final double COMMAND_RADIUS = 20.0D;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ramtarget")
                .requires(source -> source.hasPermission(0))
                .executes(RamTargetCommand::execute));
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

        Entity ram = getSelectedRam(player);
        if (ram == null) {
            ram = findRamNearTarget(player, hit.getBlockPos());
        }
        if (ram == null) {
            ram = findRamNearPlayer(player);
        }
        if (ram == null) {
            source.sendFailure(Component.literal("No ram near target or player."));
            return 0;
        }

        if (ram.getControllingPassenger() instanceof AbstractRecruitEntity mounted
                && mounted.getOwnerUUID() != null
                && mounted.getOwnerUUID().equals(player.getUUID())) {
            SiegeMachineTargets.setRamTarget(mounted.getUUID(), hit.getBlockPos());
            source.sendSuccess(() -> Component.literal("Assigned ram target to current driver."), false);
            return 1;
        }

        List<AbstractRecruitEntity> recruits = player.level().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ram.getBoundingBox().inflate(COMMAND_RADIUS),
                recruit -> recruit.getOwnerUUID() != null && recruit.getOwnerUUID().equals(player.getUUID())
        );

        Entity ramEntity = ram;
        AbstractRecruitEntity driver = recruits.stream()
                .filter(recruit -> !recruit.isPassenger())
                .min(Comparator.comparingDouble(recruit -> recruit.distanceToSqr(ramEntity)))
                .orElse(null);

        if (driver == null) {
            source.sendFailure(Component.literal("No available recruits to drive the ram."));
            return 0;
        }

        SiegeMachineTargets.setRamTarget(driver.getUUID(), hit.getBlockPos());
        source.sendSuccess(() -> Component.literal("Assigned 1 recruit to ram target."), false);
        return 1;
    }

    private static BlockHitResult getLookBlock(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(128.0D));
        HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }

    private static Entity getSelectedRam(ServerPlayer player) {
        Integer id = RamSelectionStore.get(player.getUUID());
        if (id == null) {
            return null;
        }
        return player.level().getEntity(id);
    }

    private static Entity findRamNearTarget(ServerPlayer player, BlockPos targetPos) {
        Vec3 target = Vec3.atCenterOf(targetPos);
        AABB box = new AABB(target, target).inflate(COMMAND_RADIUS);
        return player.level().getEntitiesOfClass(
                Entity.class,
                box,
                entity -> entity instanceof Useable useable && useable.getUsage() == Useable.UsageType.RAM
        ).stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(target))).orElse(null);
    }

    private static Entity findRamNearPlayer(ServerPlayer player) {
        return player.level().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(COMMAND_RADIUS),
                entity -> entity instanceof Useable useable && useable.getUsage() == Useable.UsageType.RAM
        ).stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }
}
