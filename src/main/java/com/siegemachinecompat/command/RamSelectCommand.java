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
import ru.magistu.siegemachines.api.enitity.Useable;
import java.util.List;
import net.minecraft.world.entity.Entity;

@Mod.EventBusSubscriber(modid = "siege_machine_compat")
public class RamSelectCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ramselect")
                .requires(source -> source.hasPermission(0))
                .executes(RamSelectCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        Entity ram = getLookRam(player);
        if (ram == null) {
            source.sendFailure(Component.literal("No ram in sight."));
            return 0;
        }

        RamSelectionStore.select(player.getUUID(), ram.getId());
        source.sendSuccess(() -> Component.literal("Ram selected."), false);
        return 1;
    }

    private static Entity getLookRam(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getLookAngle().scale(128.0D));
        List<Entity> rams = player.level().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(128.0D),
                machine -> machine instanceof Useable && ((Useable) machine).getUsage() == Useable.UsageType.RAM
        );
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity ram : rams) {
            Vec3 hit = ram.getBoundingBox().clip(start, end).orElse(null);
            if (hit == null) {
                continue;
            }
            double dist = start.distanceToSqr(hit);
            if (dist < bestDist) {
                bestDist = dist;
                best = ram;
            }
        }
        return best;
    }
}
