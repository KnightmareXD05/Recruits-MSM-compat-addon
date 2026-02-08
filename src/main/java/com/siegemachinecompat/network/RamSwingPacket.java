package com.siegemachinecompat.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.magistu.siegemachines.api.enitity.Useable;

import java.util.function.Supplier;

public class RamSwingPacket {

    public static RamSwingPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new RamSwingPacket();
    }

    public void encode(net.minecraft.network.FriendlyByteBuf buf) {
    }

    public static void send() {
        NetworkHandler.INSTANCE.sendToServer(new RamSwingPacket());
    }

    public static void handle(RamSwingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.level().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    player.getBoundingBox().inflate(40),
                    AbstractRecruitEntity::isPassenger
            ).forEach(recruit -> {
                if (recruit.getVehicle() instanceof Useable useable && useable.getUsage() == Useable.UsageType.RAM) {
                    useable.use(recruit);
                }
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
