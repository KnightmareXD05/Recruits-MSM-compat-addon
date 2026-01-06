package com.siegemachinecompat.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    public static final String VERSION = "1";

    public static final SimpleChannel INSTANCE =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation("siege_machine_compat", "main"),
                    () -> VERSION,
                    VERSION::equals,
                    VERSION::equals
            );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++,
                FireSiegePacket.class,
                FireSiegePacket::encode,
                FireSiegePacket::decode,
                FireSiegePacket::handle);
    }
}