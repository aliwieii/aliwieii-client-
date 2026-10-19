package com.nexora.client.gui;

import com.nexora.client.core.NexoraClient;
import com.nexora.client.hud.HudModule;
import com.nexora.client.localization.ArabicTextRenderer;
import com.nexora.client.localization.I18nManager;
import com.nexora.client.utils.GL4ESRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * NEXORA CLIENT — Interactive In-Game HUD Editor Screen (Mobile Touch & Desktop Mouse Ready).
 * Supports drag-and-drop positioning, grid snapping, auto-arrange, touch inspector bar
 * (Enable/Disable, Lock/Unlock, Scale -/+, Background toggle, Accent Color, Reset single/all),
 * and automatic screen bounds clamping.
 */
public class GuiHudEditor extends GuiScreen {

    private HudModule selectedModule = null;
    private HudModule draggingModule = null;
    private float dragOffsetX = 0.0f;
    private float dragOffsetY = 0.0f;
    private boolean showGrid = true;

    @Override
    public void initGui() {
        super.initGui();
        List<HudModule> list = NexoraClient.getInstance().getModuleManager().getHudModules();
        if (selectedModule == null && !list.isEmpty()) {
            selectedModule = list.get(0);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // Subtle dark backdrop so player sees the world behind HUD elements
        GL4ESRenderUtils.drawRect(0, 0, sw, sh, 0x550B0E14);

        if (showGrid) {
            int gridStep = 16;
            for (int gx = gridStep; gx < sw; gx += gridStep) {
                GL4ESRenderUtils.drawRect(gx, 0, gx + 0.5f, sh, 0x16FFFFFF);
            }
            for (int gy = gridStep; gy < sh; gy += gridStep) {
                GL4ESRenderUtils.drawRect(0, gy, sw, gy + 0.5f, 0x16FFFFFF);
            }
            // Center alignment lines
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

        // Render all HUD widgets in editMode (shows preview even for disabled/empty widgets)
        NexoraClient.getInstance().getModuleManager().renderAllHud(sr, true);

        // Highlight currently selected HUD widget
        if (selectedModule != null) {
            float sx = selectedModule.getX() - 1.5f;
            float sy = selectedModule.getY() - 1.5f;
            float sWidth = selectedModule.getWidth() * selectedModule.getScale() + 3.0f;
            float sHeight = selectedModule.getHeight() * selectedModule.getScale() + 3.0f;
            GL4ESRenderUtils.drawBorderedRoundedRect(sx, sy, sWidth, sHeight, 5.0f, 1.5f, 0x00000000, 0xFF00A8FF);
        }

        // Top Global Toolbar
        float topW = Math.min(340.0f, sw - 12.0f);
        float topH = 24.0f;
        float topX = (sw - topW) / 2.0f;
        float topY = 4.0f;
        GL4ESRenderUtils.drawBorderedRoundedRect(topX, topY, topW, topH, 5.0f, 1.0f, 0xEB111621, 0xFF00A8FF);

        // 4 Top Toolbar Buttons: [Grid] [Auto Arrange] [Reset All] [Back]
        float btnW = (topW - 20.0f) / 4.0f;
        drawToolbarButton(topX + 4.0f, topY + 3.0f, btnW, 18.0f, I18nManager.format("ui.grid_toggle"), showGrid ? 0xFF00A8FF : 0xFF21262D, showGrid ? 0xFF0B0E14 : 0xFFE6EDF3);
        drawToolbarButton(topX + 8.0f + btnW, topY + 3.0f, btnW, 18.0f, I18nManager.format("ui.auto_arrange"), 0xFF21262D, 0xFFE6EDF3);
        drawToolbarButton(topX + 12.0f + btnW * 2, topY + 3.0f, btnW, 18.0f, I18nManager.format("ui.reset_defaults"), 0xFF21262D, 0xFFE6EDF3);
        drawToolbarButton(topX + 16.0f + btnW * 3, topY + 3.0f, btnW, 18.0f, I18nManager.format("ui.back"), 0xFF238636, 0xFFE6EDF3);

        // Bottom Touch Inspector Bar for Selected HUD Widget
        if (selectedModule != null) {
            float botW = Math.min(370.0f, sw - 12.0f);
            float botH = 38.0f;
            float botX = (sw - botW) / 2.0f;
            float botY = sh - botH - 4.0f;
            GL4ESRenderUtils.drawBorderedRoundedRect(botX, botY, botW, botH, 5.0f, 1.0f, 0xF0111621, selectedModule.getAccentColor());

            String title = I18nManager.isRtl() ? selectedModule.getNameAr() : selectedModule.getNameEn();
            int scalePct = Math.round(selectedModule.getScale() * 100.0f);
            ArabicTextRenderer.drawString(title + " (" + scalePct + "%)", botX + 8.0f, botY + 4.0f, 0xFFE6EDF3, true);

            // 6 Touch Inspector Buttons: [ON/OFF] [Lock] [Scale -] [Scale +] [BG] [Color] [Reset]
            float ibY = botY + 17.0f;
            float ibW = (botW - 32.0f) / 7.0f;
            float curX = botX + 4.0f;

            drawToolbarButton(curX, ibY, ibW, 17.0f, selectedModule.isEnabled() ? "ON" : "OFF", selectedModule.isEnabled() ? 0xFF238636 : 0xFF21262D, 0xFFE6EDF3);
            curX += ibW + 4.0f;
            drawToolbarButton(curX, ibY, ibW, 17.0f, selectedModule.isLocked() ? "Locked" : "Free", selectedModule.isLocked() ? 0xFFF85149 : 0xFF21262D, 0xFFE6EDF3);
            curX += ibW + 4.0f;
            drawToolbarButton(curX, ibY, ibW, 17.0f, "Size -", 0xFF21262D, 0xFFE6EDF3);
            curX += ibW + 4.0f;
            drawToolbarButton(curX, ibY, ibW, 17.0f, "Size +", 0xFF21262D, 0xFFE6EDF3);
            curX += ibW + 4.0f;
            drawToolbarButton(curX, ibY, ibW, 17.0f, selectedModule.isShowBackground() ? "BG:ON" : "BG:OFF", 0xFF21262D, 0xFFE6EDF3);
            curX += ibW + 4.0f;
            drawToolbarButton(curX, ibY, ibW, 17.0f, "Color", selectedModule.getAccentColor(), 0xFF0B0E14);
            curX += ibW + 4.0f;
            drawToolbarButton(curX, ibY, ibW, 17.0f, "Reset", 0xFF21262D, 0xFFE6EDF3);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawToolbarButton(float x, float y, float w, float h, String label, int bg, int fg) {
        GL4ESRenderUtils.drawRoundedRect(x, y, w, h, 3.5f, bg);
        int tw = ArabicTextRenderer.getStringWidth(label);
        ArabicTextRenderer.drawString(label, x + (w - tw) / 2.0f, y + (h - 8.0f) / 2.0f, fg, false);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && selectedModule != null && !selectedModule.isLocked()) {
            float delta = wheel > 0 ? 0.1f : -0.1f;
            selectedModule.setScale(selectedModule.getScale() + delta);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // 1. Check Top Toolbar Buttons
        float topW = Math.min(340.0f, sw - 12.0f);
        float topX = (sw - topW) / 2.0f;
        float topY = 4.0f;
        float btnW = (topW - 20.0f) / 4.0f;
        if (mouseY >= topY + 3.0f && mouseY <= topY + 21.0f) {
            if (mouseX >= topX + 4.0f && mouseX <= topX + 4.0f + btnW) {
                showGrid = !showGrid;
                return;
            }
            if (mouseX >= topX + 8.0f + btnW && mouseX <= topX + 8.0f + btnW * 2) {
                NexoraClient.getInstance().getModuleManager().autoArrangeHud(sr);
                saveActiveProfile();
                return;
            }
            if (mouseX >= topX + 12.0f + btnW * 2 && mouseX <= topX + 12.0f + btnW * 3) {
                for (HudModule hm : NexoraClient.getInstance().getModuleManager().getHudModules()) {
                    hm.resetToDefault();
                }
                saveActiveProfile();
                return;
            }
            if (mouseX >= topX + 16.0f + btnW * 3 && mouseX <= topX + 16.0f + btnW * 4) {
                mc.displayGuiScreen(new GuiNexoraMain());
                return;
            }
        }

        // 2. Check Bottom Inspector Buttons for Selected Module
        if (selectedModule != null) {
            float botW = Math.min(370.0f, sw - 12.0f);
            float botH = 38.0f;
            float botX = (sw - botW) / 2.0f;
            float botY = sh - botH - 4.0f;
            float ibY = botY + 17.0f;
            float ibW = (botW - 32.0f) / 7.0f;
            if (mouseY >= ibY && mouseY <= ibY + 17.0f && mouseX >= botX && mouseX <= botX + botW) {
                int idx = (int) ((mouseX - (botX + 4.0f)) / (ibW + 4.0f));
                if (idx == 0) selectedModule.toggle();
                else if (idx == 1) selectedModule.setLocked(!selectedModule.isLocked());
                else if (idx == 2) selectedModule.setScale(selectedModule.getScale() - 0.1f);
                else if (idx == 3) selectedModule.setScale(selectedModule.getScale() + 0.1f);
                else if (idx == 4) selectedModule.setShowBackground(!selectedModule.isShowBackground());
                else if (idx == 5) selectedModule.cycleAccentColor();
                else if (idx == 6) selectedModule.resetToDefault();
                selectedModule.clampToScreen(sr);
                saveActiveProfile();
                return;
            }
        }

        // 3. Check HUD Widgets for Selection & Dragging
        List<HudModule> list = NexoraClient.getInstance().getModuleManager().getHudModules();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule hm = list.get(i);
            if (hm.isHovered(mouseX, mouseY)) {
                selectedModule = hm;
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
            saveActiveProfile();
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void saveActiveProfile() {
        NexoraClient.getInstance().getConfigManager().saveProfileAsync(
            NexoraClient.getInstance().getProfileManager().getActiveProfile()
        );
    }

    @Override
    public void onGuiClosed() {
        saveActiveProfile();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
