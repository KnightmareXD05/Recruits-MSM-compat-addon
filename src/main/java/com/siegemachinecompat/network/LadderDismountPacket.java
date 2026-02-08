package com.siegemachinecompat.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.magistu.siegemachines.entity.machine.LadderSeat;

import java.util.function.Supplier;

public class LadderDismountPacket {

    public static LadderDismountPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new LadderDismountPacket();
    }

    public void encode(net.minecraft.network.FriendlyByteBuf buf) {
    }

    public static void send() {
        NetworkHandler.INSTANCE.sendToServer(new LadderDismountPacket());
    }

    public static void handle(LadderDismountPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.level().getEntitiesOfClass(
                    LadderSeat.class,
                    player.getBoundingBox().inflate(40),
                    LadderSeat::isVehicle
            ).forEach(seat -> {
                if (seat.getFirstPassenger() instanceof AbstractRecruitEntity recruit
                        && recruit.getOwnerUUID() != null
                        && recruit.getOwnerUUID().equals(player.getUUID())) {
                    recruit.stopRiding();
                }
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
