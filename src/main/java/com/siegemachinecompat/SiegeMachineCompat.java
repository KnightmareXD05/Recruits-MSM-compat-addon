package com.siegemachinecompat;

import com.siegemachinecompat.network.NetworkHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("siege_machine_compat")
public class SiegeMachineCompat {

    public SiegeMachineCompat() {

        NetworkHandler.register();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

    }
}
