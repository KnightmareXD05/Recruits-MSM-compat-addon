package com.siegemachinecompat.client;

import com.siegemachinecompat.network.FireSiegePacket;
import com.siegemachinecompat.network.LadderDismountPacket;
import com.siegemachinecompat.network.RamJumpPacket;
import com.siegemachinecompat.network.RamSwingPacket;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    public static KeyMapping FIRE_SIEGE_KEY;
    public static KeyMapping RAM_SWING_KEY;
    public static KeyMapping RAM_JUMP_KEY;
    public static KeyMapping LADDER_DISMOUNT_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        FIRE_SIEGE_KEY = new KeyMapping(
                "Siege fire command key",
                GLFW.GLFW_KEY_M,
                "key.categories.gameplay"
        );
        event.register(FIRE_SIEGE_KEY);

        RAM_SWING_KEY = new KeyMapping(
                "Ram swing command key",
                GLFW.GLFW_KEY_J,
                "key.categories.gameplay"
        );
        event.register(RAM_SWING_KEY);

        RAM_JUMP_KEY = new KeyMapping(
                "Ram jump command key",
                GLFW.GLFW_KEY_K,
                "key.categories.gameplay"
        );
        event.register(RAM_JUMP_KEY);

        LADDER_DISMOUNT_KEY = new KeyMapping(
                "Ladder dismount command key",
                GLFW.GLFW_KEY_L,
                "key.categories.gameplay"
        );
        event.register(LADDER_DISMOUNT_KEY);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class KeyInput {

        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            if (FIRE_SIEGE_KEY.consumeClick()) {
                FireSiegePacket.send();
            }
            if (RAM_SWING_KEY.consumeClick()) {
                RamSwingPacket.send();
            }
            if (RAM_JUMP_KEY.consumeClick()) {
                RamJumpPacket.send();
            }
            if (LADDER_DISMOUNT_KEY.consumeClick()) {
                LadderDismountPacket.send();
            }
        }
    }
}
