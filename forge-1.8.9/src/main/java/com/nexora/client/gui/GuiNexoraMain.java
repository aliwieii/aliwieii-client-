package com.nexora.client.gui;

import com.nexora.client.core.NexoraClient;
import com.nexora.client.hud.HudModule;
import com.nexora.client.localization.ArabicTextRenderer;
import com.nexora.client.localization.I18nManager;
import com.nexora.client.modules.Module;
import com.nexora.client.modules.ModuleCategory;
import com.nexora.client.modules.ModuleManager;
import com.nexora.client.profiles.ProfileManager;
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
 * touch-drag scrolling + scroll buttons for PojavLauncher, live Performance Monitor,
 * Profile switcher, Settings customizer, and Module cards.
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

    private static final int[] HIT_COLOR_PRESETS = {
        0x66FF2A2A, // Crimson Red
        0x6600A8FF, // Electric Blue
        0x66A371F7, // Cyber Purple
        0x663FB950, // Emerald Green
        0x66D29922  // Amber Gold
    };

    private int selectedTab = 0;
    private float scrollOffset = 0.0f;
    private boolean touchDraggingScroll = false;
    private int lastTouchY = 0;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        boolean rtl = I18nManager.isRtl();
        float winW = Math.min(460.0f, sw - 16.0f);
        float winH = Math.min(285.0f, sh - 16.0f);
        float winX = (sw - winW) / 2.0f;
        float winY = (sh - winH) / 2.0f;

        // Main window container (#0B0E14 charcoal with #00A8FF subtle border)
        GL4ESRenderUtils.drawBorderedRoundedRect(winX, winY, winW, winH, 8.0f, 1.2f, 0xF20B0E14, 0x6600A8FF);

        // Sidebar position (Right side in RTL, Left side in LTR)
        float sideW = 112.0f;
        float sideX = rtl ? (winX + winW - sideW) : winX;
        GL4ESRenderUtils.drawRoundedRect(sideX, winY, sideW, winH, 8.0f, 0xFF111621);

        // NEXORA Brand Header inside Sidebar
        GL4ESRenderUtils.drawNexoraEmblem(sideX + 16, winY + 17, 15.0f, 0xFF00A8FF, 0xFFA371F7);
        ArabicTextRenderer.drawString("NEXORA", sideX + 30, winY + 13, 0xFFE6EDF3, true);

        // Sidebar Navigation Items
        float navY = winY + 34.0f;
        for (int i = 0; i < NAV_KEYS.length; i++) {
            boolean active = (selectedTab == i);
            if (active) {
                GL4ESRenderUtils.drawRoundedRect(sideX + 5, navY, sideW - 10, 20, 4.0f, 0x3300A8FF);
            }
            String label = I18nManager.format(NAV_KEYS[i]);
            ArabicTextRenderer.drawAlignedString(
                label,
                sideX + 10,
                navY + 6,
                (int) (sideW - 20),
                active ? 0xFF00A8FF : 0xFF8B949E,
                rtl
            );
            navY += 22.0f;
        }

        // Content Area
        float contentX = rtl ? (winX + 10.0f) : (winX + sideW + 10.0f);
        float contentW = winW - sideW - 20.0f;
        float contentY = winY + 10.0f;
        float contentH = winH - 20.0f;

        // Top Bar inside Content Area: Title + Scroll Up/Down Touch Buttons + Language Toggle
        GL4ESRenderUtils.drawRoundedRect(contentX, contentY, contentW, 24.0f, 5.0f, 0xFF161B22);
        String activeProfile = NexoraClient.getInstance().getProfileManager().getActiveProfile();
        String headerTitle = "NEXORA 1.0.0 · " + activeProfile;
        ArabicTextRenderer.drawString(headerTitle, contentX + 8, contentY + 8, 0xFFE6EDF3, true);

        // Touch Scroll Up / Down Buttons
        float upBtnX = contentX + contentW - 108.0f;
        float dnBtnX = contentX + contentW - 84.0f;
        GL4ESRenderUtils.drawRoundedRect(upBtnX, contentY + 3, 20.0f, 18.0f, 4.0f, 0xFF21262D);
        ArabicTextRenderer.drawString("^", upBtnX + 7, contentY + 8, 0xFFE6EDF3, false);
        GL4ESRenderUtils.drawRoundedRect(dnBtnX, contentY + 3, 20.0f, 18.0f, 4.0f, 0xFF21262D);
        ArabicTextRenderer.drawString("v", dnBtnX + 7, contentY + 8, 0xFFE6EDF3, false);

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

        float listY = contentY + 30.0f;
        float listH = contentH - 30.0f;
        GL4ESRenderUtils.enableScissor((int) contentX, (int) listY, (int) contentW, (int) listH);

        if (selectedTab == 6) {
            renderProfilesTab(contentX, listY, contentW);
        } else if (selectedTab == 8) {
            renderSettingsTab(contentX, listY, contentW);
        } else if (selectedTab == 9) {
            renderAboutTab(contentX, listY, contentW);
        } else {
            renderModulesTab(contentX, listY, contentW, rtl);
        }

        GL4ESRenderUtils.disableScissor();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderModulesTab(float contentX, float listY, float contentW, boolean rtl) {
        ModuleManager mm = NexoraClient.getInstance().getModuleManager();
        float cardY = listY + scrollOffset;

        // Live Performance Monitor Card on Home Tab (selectedTab == 0)
        if (selectedTab == 0) {
            GL4ESRenderUtils.drawBorderedRoundedRect(contentX, cardY, contentW, 42.0f, 5.0f, 1.0f, 0xFF111621, 0xFF00A8FF);
            int fps = Minecraft.getDebugFPS();
            int msWhole = (int) mm.getAvgFrameTimeMs();
            int msFrac = Math.abs((int) ((mm.getAvgFrameTimeMs() - msWhole) * 10));
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
            long maxMb = rt.maxMemory() / (1024L * 1024L);

            ArabicTextRenderer.drawString(
                "Measured FPS: " + fps + " (" + msWhole + "." + msFrac + " ms/frame)  |  RAM: " + usedMb + "/" + maxMb + " MB",
                contentX + 8,
                cardY + 7,
                0xFF3FB950,
                true
            );
            ArabicTextRenderer.drawString(
                "CPS: " + mm.getLeftCps() + " L / " + mm.getRightCps() + " R  |  HUD Widgets: " + mm.getHudModules().size() + " Active Ready",
                contentX + 8,
                cardY + 23,
                0xFF8B949E,
                false
            );
            cardY += 48.0f;
        }

        List<Module> modules = mm.getAllModules();
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
                m.isEnabled() ? 0x8800A8FF : 0x22FFFFFF
            );

            String title = rtl ? m.getNameAr() : m.getNameEn();
            String desc = rtl ? m.getDescriptionAr() : m.getDescriptionEn();
            if (m instanceof HudModule) {
                title = title + " [HUD]";
            }
            ArabicTextRenderer.drawString(title, contentX + 10, cardY + 6, 0xFFE6EDF3, true);
            ArabicTextRenderer.drawString(desc, contentX + 10, cardY + 19, 0xFF8B949E, false);

            // Toggle button on the right of card
            float toggleX = contentX + contentW - 52.0f;
            int toggleBg = m.isEnabled() ? 0xFF238636 : 0xFF21262D;
            GL4ESRenderUtils.drawRoundedRect(toggleX, cardY + 8, 44.0f, 18.0f, 4.0f, toggleBg);
            String stateText = m.isEnabled() ? I18nManager.format("ui.enabled") : I18nManager.format("ui.disabled");
            int tw = ArabicTextRenderer.getStringWidth(stateText);
            ArabicTextRenderer.drawString(stateText, toggleX + (44.0f - tw) / 2.0f, cardY + 13, 0xFFE6EDF3, false);

            cardY += 39.0f;
        }
    }

    private void renderProfilesTab(float contentX, float listY, float contentW) {
        ProfileManager pm = NexoraClient.getInstance().getProfileManager();
        List<String> profiles = pm.getProfiles();
        float cardY = listY + 4.0f;
        for (int i = 0; i < profiles.size(); i++) {
            String pName = profiles.get(i);
            boolean isActive = pName.equals(pm.getActiveProfile());
            GL4ESRenderUtils.drawBorderedRoundedRect(
                contentX,
                cardY,
                contentW,
                32.0f,
                5.0f,
                1.0f,
                0xFF161B22,
                isActive ? 0xFF00A8FF : 0x22FFFFFF
            );
            ArabicTextRenderer.drawString("Profile: " + pName, contentX + 10, cardY + 11, 0xFFE6EDF3, true);

            float btnX = contentX + contentW - 68.0f;
            GL4ESRenderUtils.drawRoundedRect(btnX, cardY + 7, 58.0f, 18.0f, 4.0f, isActive ? 0xFF00A8FF : 0xFF21262D);
            String label = isActive ? "ACTIVE" : "LOAD";
            int tw = ArabicTextRenderer.getStringWidth(label);
            ArabicTextRenderer.drawString(label, btnX + (58.0f - tw) / 2.0f, cardY + 12, isActive ? 0xFF0B0E14 : 0xFFE6EDF3, false);
            cardY += 38.0f;
        }
    }

    private void renderSettingsTab(float contentX, float listY, float contentW) {
        ModuleManager mm = NexoraClient.getInstance().getModuleManager();
        float y = listY + 4.0f;

        // Card 1: HurtCam Multiplier
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 42.0f, 5.0f, 1.0f, 0xFF161B22, 0x33FFFFFF);
        int hcPct = Math.round(mm.getHurtCamMultiplier() * 100.0f);
        ArabicTextRenderer.drawString("HurtCam Shake Intensity: " + hcPct + "%", contentX + 10, y + 7, 0xFFE6EDF3, true);
        float[] presets = { 0.0f, 0.35f, 0.70f, 1.0f };
        float bx = contentX + 10;
        for (int i = 0; i < presets.length; i++) {
            boolean sel = Math.abs(mm.getHurtCamMultiplier() - presets[i]) < 0.05f;
            GL4ESRenderUtils.drawRoundedRect(bx, y + 20, 48.0f, 16.0f, 3.5f, sel ? 0xFF00A8FF : 0xFF21262D);
            ArabicTextRenderer.drawString(Math.round(presets[i] * 100) + "%", bx + 14, y + 24, sel ? 0xFF0B0E14 : 0xFFE6EDF3, false);
            bx += 54.0f;
        }
        y += 48.0f;

        // Card 2: Hit Color Presets
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 42.0f, 5.0f, 1.0f, 0xFF161B22, 0x33FFFFFF);
        ArabicTextRenderer.drawString("Damage Hit Color Overlay Preset", contentX + 10, y + 7, 0xFFE6EDF3, true);
        String[] cNames = { "Red", "Blue", "Purple", "Green", "Gold" };
        bx = contentX + 10;
        for (int i = 0; i < HIT_COLOR_PRESETS.length; i++) {
            boolean sel = mm.getHitOverlayColor() == HIT_COLOR_PRESETS[i];
            int displayCol = HIT_COLOR_PRESETS[i] | 0xFF000000;
            GL4ESRenderUtils.drawBorderedRoundedRect(bx, y + 20, 46.0f, 16.0f, 3.5f, 1.0f, displayCol, sel ? 0xFFFFFFFF : 0xFF21262D);
            ArabicTextRenderer.drawString(cNames[i], bx + 8, y + 24, 0xFF0B0E14, false);
            bx += 52.0f;
        }
        y += 48.0f;

        // Card 3: Reset All Modules & HUD to Factory Defaults
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 34.0f, 5.0f, 1.0f, 0xFF161B22, 0x44F85149);
        ArabicTextRenderer.drawString("Factory Reset All Modules & HUD Positions", contentX + 10, y + 12, 0xFFE6EDF3, true);
        float resetBtnX = contentX + contentW - 76.0f;
        GL4ESRenderUtils.drawRoundedRect(resetBtnX, y + 8, 66.0f, 18.0f, 4.0f, 0xFFF85149);
        ArabicTextRenderer.drawString("RESET ALL", resetBtnX + 8, y + 13, 0xFFFFFFFF, false);
    }

    private void renderAboutTab(float contentX, float listY, float contentW) {
        float y = listY + 4.0f;
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 185.0f, 5.0f, 1.0f, 0xFF161B22, 0x5500A8FF);
        ArabicTextRenderer.drawString("NEXORA CLIENT v1.0.0 — Forge 1.8.9 (11.15.1.2318)", contentX + 10, y + 10, 0xFF00A8FF, true);
        ArabicTextRenderer.drawString("Target Runtime: Java 8 Bytecode (Major 52) · PojavLauncher Android", contentX + 10, y + 26, 0xFFE6EDF3, false);
        ArabicTextRenderer.drawString("GPU Renderer: GL4ES Safe Pipeline (CCW Triangle Fan · Zero Shaders)", contentX + 10, y + 40, 0xFF3FB950, false);
        ArabicTextRenderer.drawString("HUD Pipeline: 3-Layer Hook (RenderGameOverlayEvent.Post + Mixin + Tick)", contentX + 10, y + 54, 0xFFE6EDF3, false);
        ArabicTextRenderer.drawString("Arabic Engine: ICU4J ArabicShaping & Bidi + 512-Entry LRU Cache", contentX + 10, y + 68, 0xFFE6EDF3, false);
        ArabicTextRenderer.drawString("Config Storage: Atomic .tmp/.bak off-thread JSON (.minecraft/nexora/)", contentX + 10, y + 82, 0xFFE6EDF3, false);
        ArabicTextRenderer.drawString("Recommended External Mods (.minecraft/mods/1.8.9/):", contentX + 10, y + 104, 0xFFA371F7, true);
        ArabicTextRenderer.drawString("1. OptiFine 1.8.9 HD U M5 (Fast Render OFF for HUD compatibility)", contentX + 10, y + 120, 0xFF8B949E, false);
        ArabicTextRenderer.drawString("2. Patcher 1.8.9 & MouseDelayFix 1.0 (Fully compatible)", contentX + 10, y + 134, 0xFF8B949E, false);
        ArabicTextRenderer.drawString("Shortcuts: [Right Shift] Open Menu  |  [H] Open HUD Editor", contentX + 10, y + 154, 0xFFD29922, true);
    }

    private boolean matchesTab(Module m, int tab) {
        if (tab == 0 || tab == 7) return true;
        if (tab == 2) return m.getCategory() == ModuleCategory.PVP || m.getCategory() == ModuleCategory.HUD;
        if (tab == 3) return m.getCategory() == ModuleCategory.PERFORMANCE;
        if (tab == 4) return m.getCategory() == ModuleCategory.VISUALS;
        if (tab == 5) return m.getCategory() == ModuleCategory.INVENTORY;
        return true;
    }

    private void scrollBy(float delta) {
        scrollOffset += delta;
        if (scrollOffset > 0.0f) {
            scrollOffset = 0.0f;
        }
        float minScroll = -1150.0f;
        if (scrollOffset < minScroll) {
            scrollOffset = minScroll;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollBy(wheel > 0 ? 24.0f : -24.0f);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (touchDraggingScroll && clickedMouseButton == 0) {
            int dy = mouseY - lastTouchY;
            lastTouchY = mouseY;
            scrollBy(dy * 1.2f);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        touchDraggingScroll = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        boolean rtl = I18nManager.isRtl();

        float winW = Math.min(460.0f, sw - 16.0f);
        float winH = Math.min(285.0f, sh - 16.0f);
        float winX = (sw - winW) / 2.0f;
        float winY = (sh - winH) / 2.0f;
        float sideW = 112.0f;
        float sideX = rtl ? (winX + winW - sideW) : winX;

        // 1. Check Sidebar clicks
        float navY = winY + 34.0f;
        for (int i = 0; i < NAV_KEYS.length; i++) {
            if (mouseX >= sideX + 5 && mouseX <= sideX + sideW - 5 && mouseY >= navY && mouseY <= navY + 20) {
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

        float contentX = rtl ? (winX + 10.0f) : (winX + sideW + 10.0f);
        float contentW = winW - sideW - 20.0f;
        float contentY = winY + 10.0f;

        // 2. Check Top Bar buttons (Scroll Up, Scroll Down, Language Switch)
        float upBtnX = contentX + contentW - 108.0f;
        float dnBtnX = contentX + contentW - 84.0f;
        float langBtnX = contentX + contentW - 58.0f;
        if (mouseY >= contentY + 3 && mouseY <= contentY + 21) {
            if (mouseX >= upBtnX && mouseX <= upBtnX + 20.0f) {
                scrollBy(60.0f);
                return;
            }
            if (mouseX >= dnBtnX && mouseX <= dnBtnX + 20.0f) {
                scrollBy(-60.0f);
                return;
            }
            if (mouseX >= langBtnX && mouseX <= langBtnX + 52.0f) {
                I18nManager.Language next = (I18nManager.getCurrentLanguage() == I18nManager.Language.AR)
                    ? I18nManager.Language.EN
                    : I18nManager.Language.AR;
                I18nManager.setCurrentLanguage(next);
                saveActiveProfile();
                return;
            }
        }

        float listY = contentY + 30.0f;

        // 3. Profiles Tab clicks
        if (selectedTab == 6) {
            ProfileManager pm = NexoraClient.getInstance().getProfileManager();
            List<String> profiles = pm.getProfiles();
            float cardY = listY + 4.0f;
            for (int i = 0; i < profiles.size(); i++) {
                if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= cardY && mouseY <= cardY + 32.0f) {
                    pm.switchProfile(profiles.get(i));
                    return;
                }
                cardY += 38.0f;
            }
            return;
        }

        // 4. Settings Tab clicks
        if (selectedTab == 8) {
            ModuleManager mm = NexoraClient.getInstance().getModuleManager();
            float y = listY + 4.0f;
            float[] presets = { 0.0f, 0.35f, 0.70f, 1.0f };
            float bx = contentX + 10;
            for (int i = 0; i < presets.length; i++) {
                if (mouseX >= bx && mouseX <= bx + 48.0f && mouseY >= y + 20 && mouseY <= y + 36) {
                    mm.setHurtCamMultiplier(presets[i]);
                    saveActiveProfile();
                    return;
                }
                bx += 54.0f;
            }
            y += 48.0f;
            bx = contentX + 10;
            for (int i = 0; i < HIT_COLOR_PRESETS.length; i++) {
                if (mouseX >= bx && mouseX <= bx + 46.0f && mouseY >= y + 20 && mouseY <= y + 36) {
                    mm.setHitOverlayColor(HIT_COLOR_PRESETS[i]);
                    saveActiveProfile();
                    return;
                }
                bx += 52.0f;
            }
            y += 48.0f;
            float resetBtnX = contentX + contentW - 76.0f;
            if (mouseX >= resetBtnX && mouseX <= resetBtnX + 66.0f && mouseY >= y + 8 && mouseY <= y + 26) {
                for (Module m : mm.getAllModules()) {
                    m.resetToDefault();
                }
                saveActiveProfile();
                return;
            }
            return;
        }

        if (selectedTab == 9) {
            return;
        }

        // 5. Module Toggle clicks & Touch-Scroll initiation
        float cardY = listY + scrollOffset;
        if (selectedTab == 0) {
            cardY += 48.0f;
        }
        List<Module> modules = NexoraClient.getInstance().getModuleManager().getAllModules();
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (!matchesTab(m, selectedTab)) continue;
            float toggleX = contentX + contentW - 52.0f;
            if (mouseX >= toggleX && mouseX <= toggleX + 44.0f && mouseY >= cardY + 8 && mouseY <= cardY + 26) {
                m.toggle();
                saveActiveProfile();
                return;
            }
            cardY += 39.0f;
        }

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= listY) {
            touchDraggingScroll = true;
            lastTouchY = mouseY;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void saveActiveProfile() {
        NexoraClient.getInstance().getConfigManager().saveProfileAsync(
            NexoraClient.getInstance().getProfileManager().getActiveProfile()
        );
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
