package com.nexora.client.mixin;

import com.nexora.client.core.NexoraClient;
import com.nexora.client.gui.GuiHudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NEXORA CLIENT — SpongePowered Mixin 0.7.11 for GuiIngame.
 * Renders enabled NEXORA HUD elements during the game overlay phase.
 */
@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderGameOverlay", at = @At("RETURN"))
    private void nexora$onRenderGameOverlay(float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || mc.currentScreen instanceof GuiHudEditor) {
            return;
        }
        NexoraClient client = NexoraClient.getInstance();
        if (client != null && client.getModuleManager() != null) {
            ScaledResolution sr = new ScaledResolution(mc);
            client.getModuleManager().renderAllHud(sr, false);
        }
    }
}
