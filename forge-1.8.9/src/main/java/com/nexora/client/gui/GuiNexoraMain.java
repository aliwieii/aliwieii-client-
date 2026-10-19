package com.nexora.client.gui;

import com.nexora.client.core.NexoraClient;
import com.nexora.client.localization.ArabicTextRenderer;
import com.nexora.client.localization.I18nManager;
import com.nexora.client.modules.Module;
import com.nexora.client.modules.ModuleCategory;
import com.nexora.client.utils.GL4ESRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * NEXORA CLIENT — Main In-Game Settings & Dashboard GUI.
 * Features dark charcoal & electric blue aesthetic, RTL/LTR layout adaptation,
 * sidebar navigation, scrollable module cards, and touch/mouse responsiveness.
 */
public class GuiNexoraMain extends GuiScreen {

    private static final String[] NAV_KEYS = {
        "nav.home",
        "nav.hud_editor",
        "nav.pvp",
        "nav.performance",
        "nav.visuals",
        "nav.inventory",
        "nav.profiles",
        "nav.mods",
        "nav.settings",
        "nav.about"
    };

    private int selectedTab = 0;
    private float scrollOffset = 0.0f;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        boolean rtl = I18nManager.isRtl();
        float winW = Math.min(460.0f, sw - 24.0f);
        float winH = Math.min(280.0f, sh - 24.0f);
        float winX = (sw - winW) / 2.0f;
        float winY = (sh - winH) / 2.0f;

        // Main window container (#0B0E14 charcoal with #00A8FF subtle border)
        GL4ESRenderUtils.drawBorderedRoundedRect(winX, winY, winW, winH, 8.0f, 1.2f, 0xF20B0E14, 0x5500A8FF);

        // Sidebar position (Right side in RTL, Left side in LTR)
        float sideW = 115.0f;
        float sideX = rtl ? (winX + winW - sideW) : winX;
        GL4ESRenderUtils.drawRoundedRect(sideX, winY, sideW, winH, 8.0f, 0xFF111621);

        // NEXORA Brand Header inside Sidebar
        GL4ESRenderUtils.drawNexoraEmblem(sideX + 18, winY + 18, 16.0f, 0xFF00A8FF, 0xFFA371F7);
        ArabicTextRenderer.drawString("NEXORA", sideX + 32, winY + 14, 0xFFE6EDF3, true);

        // Sidebar Navigation Items
        float navY = winY + 38.0f;
        for (int i = 0; i < NAV_KEYS.length; i++) {
            boolean active = (selectedTab == i);
            if (active) {
                GL4ESRenderUtils.drawRoundedRect(sideX + 6, navY, sideW - 12, 20, 4.0f, 0x3300A8FF);
            }
            String label = I18nManager.format(NAV_KEYS[i]);
            ArabicTextRenderer.drawAlignedString(
                label,
                sideX + 12,
                navY + 6,
                (int) (sideW - 24),
                active ? 0xFF00A8FF : 0xFF8B949E,
                rtl
            );
            navY += 22.0f;
        }

        // Content Area
        float contentX = rtl ? (winX + 12.0f) : (winX + sideW + 12.0f);
        float contentW = winW - sideW - 24.0f;
        float contentY = winY + 12.0f;
        float contentH = winH - 24.0f;

        // Top Bar inside Content Area: Language Toggle + Active Profile
        GL4ESRenderUtils.drawRoundedRect(contentX, contentY, contentW, 24.0f, 5.0f, 0xFF161B22);
        String headerTitle = "NEXORA CLIENT 1.0.0 (Forge 1.8.9)";
        ArabicTextRenderer.drawString(headerTitle, contentX + 8, contentY + 8, 0xFFE6EDF3, true);

        // Language switch button
        float langBtnX = contentX + contentW - 58.0f;
        GL4ESRenderUtils.drawRoundedRect(langBtnX, contentY + 3, 52.0f, 18.0f, 4.0f, 0xFF00A8FF);
        ArabicTextRenderer.drawString(
            I18nManager.getCurrentLanguage().getDisplayName(),
            langBtnX + 8,
            contentY + 8,
            0xFF0B0E14,
            false
        );

        // Render Module Cards inside Scissor Box
        float listY = contentY + 32.0f;
        float listH = contentH - 32.0f;
        GL4ESRenderUtils.enableScissor((int) contentX, (int) listY, (int) contentW, (int) listH);

        float cardY = listY + scrollOffset;
        List<Module> modules = NexoraClient.getInstance().getModuleManager().getAllModules();
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (!matchesTab(m, selectedTab)) {
                continue;
            }
            GL4ESRenderUtils.drawBorderedRoundedRect(
                contentX,
                cardY,
                contentW,
                34.0f,
                5.0f,
                1.0f,
                0xFF161B22,
                m.isEnabled() ? 0x6600A8FF : 0x22FFFFFF
            );

            String title = rtl ? m.getNameAr() : m.getNameEn();
            String desc = rtl ? m.getDescriptionAr() : m.getDescriptionEn();
            ArabicTextRenderer.drawString(title, contentX + 10, cardY + 6, 0xFFE6EDF3, true);
            ArabicTextRenderer.drawString(desc, contentX + 10, cardY + 19, 0xFF8B949E, false);

            // Toggle button on the right of card
            float toggleX = contentX + contentW - 54.0f;
            int toggleBg = m.isEnabled() ? 0xFF238636 : 0xFF21262D;
            GL4ESRenderUtils.drawRoundedRect(toggleX, cardY + 8, 44.0f, 18.0f, 4.0f, toggleBg);
            String stateText = m.isEnabled() ? I18nManager.format("ui.enabled") : I18nManager.format("ui.disabled");
            ArabicTextRenderer.drawString(stateText, toggleX + 8, cardY + 13, 0xFFE6EDF3, false);

            cardY += 40.0f;
        }

        GL4ESRenderUtils.disableScissor();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private boolean matchesTab(Module m, int tab) {
        if (tab == 0 || tab == 7) return true;
        if (tab == 2) return m.getCategory() == ModuleCategory.PVP || m.getCategory() == ModuleCategory.HUD;
        if (tab == 3) return m.getCategory() == ModuleCategory.PERFORMANCE;
        if (tab == 4) return m.getCategory() == ModuleCategory.VISUALS;
        if (tab == 5) return m.getCategory() == ModuleCategory.INVENTORY;
        return true;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollOffset += (wheel > 0 ? 18.0f : -18.0f);
            if (scrollOffset > 0.0f) scrollOffset = 0.0f;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        boolean rtl = I18nManager.isRtl();

        float winW = Math.min(460.0f, sw - 24.0f);
        float winH = Math.min(280.0f, sh - 24.0f);
        float winX = (sw - winW) / 2.0f;
        float winY = (sh - winH) / 2.0f;
        float sideW = 115.0f;
        float sideX = rtl ? (winX + winW - sideW) : winX;

        // Check Sidebar clicks
        float navY = winY + 38.0f;
        for (int i = 0; i < NAV_KEYS.length; i++) {
            if (mouseX >= sideX + 6 && mouseX <= sideX + sideW - 6 && mouseY >= navY && mouseY <= navY + 20) {
                if (i == 1) {
                    Minecraft.getMinecraft().displayGuiScreen(new GuiHudEditor());
                    return;
                }
                selectedTab = i;
                scrollOffset = 0.0f;
                return;
            }
            navY += 22.0f;
        }

        // Check Language Switch click
        float contentX = rtl ? (winX + 12.0f) : (winX + sideW + 12.0f);
        float contentW = winW - sideW - 24.0f;
        float contentY = winY + 12.0f;
        float langBtnX = contentX + contentW - 58.0f;
        if (mouseX >= langBtnX && mouseX <= langBtnX + 52.0f && mouseY >= contentY + 3 && mouseY <= contentY + 21) {
            I18nManager.Language next = (I18nManager.getCurrentLanguage() == I18nManager.Language.AR)
                ? I18nManager.Language.EN
                : I18nManager.Language.AR;
            I18nManager.setCurrentLanguage(next);
            return;
        }

        // Check Module Toggle clicks
        float listY = contentY + 32.0f;
        float cardY = listY + scrollOffset;
        List<Module> modules = NexoraClient.getInstance().getModuleManager().getAllModules();
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (!matchesTab(m, selectedTab)) continue;
            float toggleX = contentX + contentW - 54.0f;
            if (mouseX >= toggleX && mouseX <= toggleX + 44.0f && mouseY >= cardY + 8 && mouseY <= cardY + 26) {
                m.toggle();
                NexoraClient.getInstance().getConfigManager().saveProfileAsync(
                    NexoraClient.getInstance().getProfileManager().getActiveProfile()
                );
                return;
            }
            cardY += 40.0f;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
