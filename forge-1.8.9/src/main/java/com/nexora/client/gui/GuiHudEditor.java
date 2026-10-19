package com.nexora.client.gui;

import com.nexora.client.core.NexoraClient;
import com.nexora.client.hud.HudModule;
import com.nexora.client.localization.ArabicTextRenderer;
import com.nexora.client.localization.I18nManager;
import com.nexora.client.utils.GL4ESRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.io.IOException;
import java.util.List;

/**
 * NEXORA CLIENT — Interactive In-Game HUD Editor Screen.
 * Supports drag-and-drop positioning, grid snapping, lock toggle, reset defaults,
 * and automatic screen bounds clamping for mobile & desktop aspect ratios.
 */
public class GuiHudEditor extends GuiScreen {

    private HudModule draggingModule = null;
    private float dragOffsetX = 0.0f;
    private float dragOffsetY = 0.0f;
    private boolean showGrid = true;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // Subtle dark backdrop so player can still see the world behind HUD elements
        GL4ESRenderUtils.drawRect(0, 0, sw, sh, 0x550B0E14);

        if (showGrid) {
            int gridStep = 16;
            for (int gx = gridStep; gx < sw; gx += gridStep) {
                GL4ESRenderUtils.drawRect(gx, 0, gx + 0.5f, sh, 0x18FFFFFF);
            }
            for (int gy = gridStep; gy < sh; gy += gridStep) {
                GL4ESRenderUtils.drawRect(0, gy, sw, gy + 0.5f, 0x18FFFFFF);
            }
            // Center crosshair lines
            GL4ESRenderUtils.drawRect(sw / 2.0f - 0.5f, 0, sw / 2.0f + 0.5f, sh, 0x4400A8FF);
            GL4ESRenderUtils.drawRect(0, sh / 2.0f - 0.5f, sw, sh / 2.0f + 0.5f, 0x4400A8FF);
        }

        if (draggingModule != null && !draggingModule.isLocked()) {
            float targetX = mouseX - dragOffsetX;
            float targetY = mouseY - dragOffsetY;
            if (showGrid) {
                targetX = Math.round(targetX / 4.0f) * 4.0f;
                targetY = Math.round(targetY / 4.0f) * 4.0f;
            }
            draggingModule.setX(targetX);
            draggingModule.setY(targetY);
            draggingModule.clampToScreen(sr);
        }

        NexoraClient.getInstance().getModuleManager().renderAllHud(sr, true);

        // Top toolbar
        float barW = 280.0f;
        float barH = 26.0f;
        float barX = (sw - barW) / 2.0f;
        float barY = 8.0f;
        GL4ESRenderUtils.drawBorderedRoundedRect(barX, barY, barW, barH, 6.0f, 1.0f, 0xE6111621, 0xFF00A8FF);

        ArabicTextRenderer.drawString(
            I18nManager.format("nav.hud_editor") + " — NEXORA",
            barX + 12,
            barY + 9,
            0xFFE6EDF3,
            true
        );

        // Grid toggle button
        float btnGridX = barX + barW - 135;
        GL4ESRenderUtils.drawRoundedRect(btnGridX, barY + 4, 60, 18, 4.0f, showGrid ? 0xFF00A8FF : 0xFF21262D);
        ArabicTextRenderer.drawString(
            I18nManager.format("ui.grid_toggle"),
            btnGridX + 6,
            barY + 9,
            showGrid ? 0xFF0B0E14 : 0xFFE6EDF3,
            false
        );

        // Reset defaults button
        float btnResetX = barX + barW - 68;
        GL4ESRenderUtils.drawRoundedRect(btnResetX, barY + 4, 62, 18, 4.0f, 0xFF21262D);
        ArabicTextRenderer.drawString(
            I18nManager.format("ui.reset_defaults"),
            btnResetX + 6,
            barY + 9,
            0xFFE6EDF3,
            false
        );

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        float barW = 280.0f;
        float barX = (sw - barW) / 2.0f;
        float barY = 8.0f;

        // Toolbar buttons check
        float btnGridX = barX + barW - 135;
        if (mouseX >= btnGridX && mouseX <= btnGridX + 60 && mouseY >= barY + 4 && mouseY <= barY + 22) {
            showGrid = !showGrid;
            return;
        }
        float btnResetX = barX + barW - 68;
        if (mouseX >= btnResetX && mouseX <= btnResetX + 62 && mouseY >= barY + 4 && mouseY <= barY + 22) {
            for (HudModule hm : NexoraClient.getInstance().getModuleManager().getHudModules()) {
                hm.resetToDefault();
            }
            return;
        }

        List<HudModule> list = NexoraClient.getInstance().getModuleManager().getHudModules();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule hm = list.get(i);
            if (hm.isHovered(mouseX, mouseY)) {
                if (mouseButton == 0 && !hm.isLocked()) {
                    draggingModule = hm;
                    dragOffsetX = mouseX - hm.getX();
                    dragOffsetY = mouseY - hm.getY();
                } else if (mouseButton == 1) {
                    hm.setLocked(!hm.isLocked());
                }
                break;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggingModule != null) {
            draggingModule = null;
            NexoraClient.getInstance().getConfigManager().saveProfileAsync(
                NexoraClient.getInstance().getProfileManager().getActiveProfile()
            );
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void onGuiClosed() {
        NexoraClient.getInstance().getConfigManager().saveProfileAsync(
            NexoraClient.getInstance().getProfileManager().getActiveProfile()
        );
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
