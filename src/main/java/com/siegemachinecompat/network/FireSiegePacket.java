package com.siegemachinecompat.network;

import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.magistu.siegemachines.api.enitity.Shootable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class FireSiegePacket {

    // avoid potential OutOfMemoryError
    private static final Map<UUID, Long> LAST_FIRED_CACHE = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) {
            return size() > 16;
        }
    });

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
            long last = LAST_FIRED_CACHE.getOrDefault(id, 0L);

            if (gameTime - last < 8)  // 0.4 seconds
                return;

            LAST_FIRED_CACHE.put(id, gameTime);

            player.level().getEntitiesOfClass(
                    BowmanEntity.class,
                    player.getBoundingBox().inflate(40),
                    BowmanEntity::isPassenger
            ).forEach(bowman -> {
                if (bowman.getVehicle() instanceof Shootable shootable) {
                    if (!shootable.hasAmmo() && bowman.getOwner() != null) {
                        bowman.getOwner().sendSystemMessage(Component.translatable("siege_machine_compat.no_ammo", bowman.getDisplayName(), shootable.asLivingEntity().getDisplayName()));
                        return;
                    }
                    shootable.use(bowman);
                }
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
