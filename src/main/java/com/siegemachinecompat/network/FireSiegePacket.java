package com.siegemachinecompat.network;

import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class FireSiegePacket {

    private static final Map<UUID, Long> LAST_FIRE = new HashMap<>();

    public static FireSiegePacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new FireSiegePacket();
    }

    public void encode(net.minecraft.network.FriendlyByteBuf buf) {}

    public static void send() {
        NetworkHandler.INSTANCE.sendToServer(new FireSiegePacket());
    }

    public static void handle(FireSiegePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            long gameTime = player.level().getGameTime();

            UUID id = player.getUUID();
            long last = LAST_FIRE.getOrDefault(id, 0L);

            if (gameTime - last < 8)  // 0.4 seconds
                return;

            LAST_FIRE.put(id, gameTime);

            player.level().getEntitiesOfClass(
                    BowmanEntity.class,
                    player.getBoundingBox().inflate(40),
                    BowmanEntity::isPassenger
            ).forEach(bowman -> {

                if (bowman.getVehicle() instanceof ru.magistu.siegemachines.entity.machine.ShootingMachine sm) {

                    if (!sm.hasAmmo()) return;

                    try {
                        var d = sm.getClass().getField("delayticks");
                        var u = sm.getClass().getField("useticks");
                        var s = sm.getClass().getField("shootingticks");

                        if ((int) d.get(sm) > 0) return;
                        if ((int) u.get(sm) > 0) return;
                        if ((int) s.get(sm) > 0) return;

                    } catch (Exception ignored) {}


                    sm.use(bowman);
                }
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
