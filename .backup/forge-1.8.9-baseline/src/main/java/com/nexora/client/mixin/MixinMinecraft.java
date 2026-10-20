package com.nexora.client.mixin;

import com.nexora.client.core.NexoraClient;
import com.nexora.client.modules.ModuleManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NEXORA CLIENT — SpongePowered Mixin 0.7.11 for Minecraft.
 * Fixes the infamous Minecraft 1.8.9 10-tick hit delay bug (leftClickCounter / field_71429_W)
 * where swinging and missing sets leftClickCounter = 10, blocking attack registration for 0.5s.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    private int leftClickCounter;

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void nexora$onClickMouseHead(CallbackInfo ci) {
        NexoraClient client = NexoraClient.getInstance();
        if (client != null) {
            ModuleManager mm = client.getModuleManager();
            if (mm != null && mm.isHitDelayFixEnabled()) {
                this.leftClickCounter = 0;
            }
        }
    }
}
