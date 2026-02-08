package com.siegemachinecompat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.magistu.siegemachines.entity.machine.SiegeLadder;

import java.util.List;

@Mod.EventBusSubscriber(modid = "siege_machine_compat")
public class LadderSelectCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ladderselect")
                .requires(source -> source.hasPermission(0))
                .executes(LadderSelectCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        SiegeLadder ladder = getLookLadder(player);
        if (ladder == null) {
            source.sendFailure(Component.literal("No siege ladder in sight."));
            return 0;
        }

        LadderSelectionStore.select(player.getUUID(), ladder.getId());
        source.sendSuccess(() -> Component.literal("Ladder selected."), false);
        return 1;
    }

    private static SiegeLadder getLookLadder(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(128.0D));
        List<SiegeLadder> ladders = player.level().getEntitiesOfClass(
                SiegeLadder.class,
                player.getBoundingBox().inflate(128.0D),
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
}
