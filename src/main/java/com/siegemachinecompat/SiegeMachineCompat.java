package com.siegemachinecompat;

import com.siegemachinecompat.network.NetworkHandler;
import net.minecraftforge.fml.common.Mod;

@Mod("siege_machine_compat")
public class SiegeMachineCompat {

    public SiegeMachineCompat() {
        NetworkHandler.register();
    }
}
