package com.nexora.client.mixin;

import com.nexora.client.core.NexoraClient;
import com.nexora.client.modules.ModuleManager;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * NEXORA CLIENT — SpongePowered Mixin 0.7.11 for EntityRenderer.
 * Controls HurtCam tilt intensity without breaking vanilla camera transforms.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @ModifyArg(
        method = "hurtCameraEffect",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"),
        index = 0
    )
    private float nexora$modifyHurtCamAngle(float angle) {
        NexoraClient client = NexoraClient.getInstance();
        if (client != null) {
            ModuleManager mm = client.getModuleManager();
            if (mm != null && mm.isHurtCamEnabled()) {
                return angle * mm.getHurtCamMultiplier();
            }
        }
        return angle;
    }
}
