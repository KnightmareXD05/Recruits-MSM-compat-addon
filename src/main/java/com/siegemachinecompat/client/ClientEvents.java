package com.siegemachinecompat.client;

import com.siegemachinecompat.network.FireSiegePacket;
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

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        FIRE_SIEGE_KEY = new KeyMapping(
                "Siege fire command key",
                GLFW.GLFW_KEY_M,
                "key.categories.gameplay"
        );
        event.register(FIRE_SIEGE_KEY);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class KeyInput {

        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            if (FIRE_SIEGE_KEY.consumeClick()) {
                FireSiegePacket.send();
            }
        }
    }
}
