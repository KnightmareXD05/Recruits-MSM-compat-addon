package com.siegemachinecompat.client;

import com.siegemachinecompat.network.LadderDismountPacket;
import com.siegemachinecompat.network.RamJumpPacket;
import com.siegemachinecompat.network.RamSwingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class LadderRamScreen extends Screen {

    public LadderRamScreen() {
        super(Component.literal("Siege Commands"));
    }

    @Override
    protected void init() {
        int buttonWidth = 160;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;
        int gap = 4;

        addRenderableWidget(Button.builder(Component.literal("Ladder Select"), button -> runCommand("ladderselect"))
                .bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Ladder Target"), button -> runCommand("laddertarget"))
                .bounds(centerX - buttonWidth / 2, startY + (buttonHeight + gap), buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Ladder Climb"), button -> runCommand("ladderclimb"))
                .bounds(centerX - buttonWidth / 2, startY + 2 * (buttonHeight + gap), buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Ladder Dismount (L)"), button -> LadderDismountPacket.send())
                .bounds(centerX - buttonWidth / 2, startY + 3 * (buttonHeight + gap), buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Ram Select"), button -> runCommand("ramselect"))
                .bounds(centerX - buttonWidth / 2, startY + 4 * (buttonHeight + gap), buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Ram Target"), button -> runCommand("ramtarget"))
                .bounds(centerX - buttonWidth / 2, startY + 5 * (buttonHeight + gap), buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Ram Swing (J)"), button -> RamSwingPacket.send())
                .bounds(centerX - buttonWidth / 2, startY + 6 * (buttonHeight + gap), buttonWidth, buttonHeight).build());
        addRenderableWidget(Button.builder(Component.literal("Ram Jump (K)"), button -> RamJumpPacket.send())
                .bounds(centerX - buttonWidth / 2, startY + 7 * (buttonHeight + gap), buttonWidth, buttonHeight).build());
    }

    private void runCommand(String command) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.connection != null) {
            Minecraft.getInstance().player.connection.sendCommand(command);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 100, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_B) {
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
