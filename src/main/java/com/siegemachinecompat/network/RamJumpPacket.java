package com.siegemachinecompat.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraftforge.network.NetworkEvent;
import ru.magistu.siegemachines.api.enitity.Useable;

import java.util.function.Supplier;

public class RamJumpPacket {

    public static RamJumpPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new RamJumpPacket();
    }

    public void encode(net.minecraft.network.FriendlyByteBuf buf) {
    }

    public static void send() {
        NetworkHandler.INSTANCE.sendToServer(new RamJumpPacket());
    }

    public static void handle(RamJumpPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.level().getEntitiesOfClass(
                    Entity.class,
                    player.getBoundingBox().inflate(40),
                    entity -> entity instanceof Useable useable && useable.getUsage() == Useable.UsageType.RAM
            ).forEach(ram -> {
                if (ram.getControllingPassenger() instanceof AbstractRecruitEntity recruit
                        && recruit.getOwnerUUID() != null
                        && recruit.getOwnerUUID().equals(player.getUUID())) {
                    ram.move(MoverType.SELF, ram.getDeltaMovement().add(0.0D, 2.0D, 0.0D));
                }
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
