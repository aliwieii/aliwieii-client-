package com.nexora.client.mixin;

import com.nexora.client.core.NexoraClient;
import com.nexora.client.modules.ModuleManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * NEXORA CLIENT — SpongePowered Mixin 0.7.11 for RendererLivingEntity.
 * Allows customizing the damage hit color overlay on living entities.
 */
@Mixin(RendererLivingEntity.class)
public class MixinRendererLivingEntity {

    @ModifyArgs(
        method = "setBrightness",
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0),
        require = 0
    )
    private void nexora$modifyHitColorRed(Args args) {
        NexoraClient client = NexoraClient.getInstance();
        if (client != null && client.getModuleManager() != null) {
            ModuleManager mm = client.getModuleManager();
            if (mm.isHitColorEnabled()) {
                int color = mm.getHitOverlayColor();
                float r = (float) (color >> 16 & 255) / 255.0F;
                args.set(0, r);
            }
        }
    }
}
