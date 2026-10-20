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
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * NEXORA CLIENT — Modern In-Game Settings & Dashboard GUI (v2 Redesign).
 * Features dark charcoal & selectable theme accent palette, RTL/LTR layout adaptation,
 * interactive live Search Box, Quick Status Filter (All / Active / HUD),
 * smooth interpolated scrolling + touch scroll buttons for PojavLauncher,
 * modern pill Toggle Switches, UI sound feedback, and Toast Notifications.
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

    private static final int[] THEME_ACCENT_PRESETS = {
        0xFF00A8FF, // Electric Blue
        0xFFA371F7, // Cyber Purple
        0xFF3FB950, // Emerald Green
        0xFFF85149, // Crimson Strike
        0xFFD29922  // Amber Gold
    };

    private int selectedTab = 0;
    private float scrollOffset = 0.0f;
    private float targetScrollOffset = 0.0f;
    private boolean touchDraggingScroll = false;
    private int lastTouchY = 0;

    // Live Search & Quick Filter State
    private String searchQuery = "";
    private boolean searchFocused = false;
    private int quickFilterMode = 0; // 0 = All, 1 = Enabled Only, 2 = HUD Only

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        ModuleManager mm = NexoraClient.getInstance().getModuleManager();
        int themeAccent = mm.getClientThemeColor();

        // Smooth scroll interpolation
        scrollOffset = GL4ESRenderUtils.lerp(scrollOffset, targetScrollOffset, 0.35f);

        boolean rtl = I18nManager.isRtl();
        float winW = Math.min(468.0f, sw - 14.0f);
        float winH = Math.min(290.0f, sh - 14.0f);
        float winX = (sw - winW) / 2.0f;
        float winY = (sh - winH) / 2.0f;

        // Main window container (#0B0E14 charcoal with theme accent subtle border)
        int winBorder = (themeAccent & 0x00FFFFFF) | 0x77000000;
        GL4ESRenderUtils.drawBorderedRoundedRect(winX, winY, winW, winH, 8.0f, 1.2f, 0xF20B0E14, winBorder);

        // Sidebar position (Right side in RTL, Left side in LTR)
        float sideW = 112.0f;
        float sideX = rtl ? (winX + winW - sideW) : winX;
        GL4ESRenderUtils.drawRoundedRect(sideX, winY, sideW, winH, 8.0f, 0xFF111621);

        // NEXORA Brand Header inside Sidebar
        GL4ESRenderUtils.drawNexoraEmblem(sideX + 16, winY + 17, 15.0f, themeAccent, 0xFFA371F7);
        ArabicTextRenderer.drawString("NEXORA", sideX + 30, winY + 13, 0xFFE6EDF3, true);

        // Sidebar Navigation Items
        float navY = winY + 34.0f;
        for (int i = 0; i < NAV_KEYS.length; i++) {
            boolean active = (selectedTab == i);
            if (active) {
                int activeBg = (themeAccent & 0x00FFFFFF) | 0x33000000;
                GL4ESRenderUtils.drawRoundedRect(sideX + 5, navY, sideW - 10, 20, 4.0f, activeBg);
            }
            String label = I18nManager.format(NAV_KEYS[i]);
            ArabicTextRenderer.drawAlignedString(
                label,
                sideX + 10,
                navY + 6,
                (int) (sideW - 20),
                active ? themeAccent : 0xFF8B949E,
                rtl
            );
            navY += 22.0f;
        }

        // Content Area
        float contentX = rtl ? (winX + 10.0f) : (winX + sideW + 10.0f);
        float contentW = winW - sideW - 20.0f;
        float contentY = winY + 8.0f;
        float contentH = winH - 16.0f;

        // Top Bar inside Content Area: Search Input + Quick Filter + Scroll Up/Down + Language Toggle
        GL4ESRenderUtils.drawRoundedRect(contentX, contentY, contentW, 24.0f, 5.0f, 0xFF161B22);

        // Interactive Search Box (Left side of Top Bar)
        float searchX = contentX + 4.0f;
        float searchW = contentW - 156.0f;
        int searchBorder = searchFocused ? themeAccent : 0x448B949E;
        GL4ESRenderUtils.drawBorderedRoundedRect(searchX, contentY + 3, searchW, 18.0f, 4.0f, 1.0f, 0xFF0B0E14, searchBorder);
        String searchDisplay;
        int searchTextColor;
        if (searchQuery.isEmpty() && !searchFocused) {
            searchDisplay = I18nManager.format("ui.search_placeholder");
            searchTextColor = 0xFF8B949E;
        } else {
            String cursor = (searchFocused && (System.currentTimeMillis() / 450L) % 2L == 0L) ? "_" : "";
            searchDisplay = searchQuery + cursor;
            searchTextColor = 0xFFE6EDF3;
        }
        ArabicTextRenderer.drawString(searchDisplay, searchX + 6.0f, contentY + 8.0f, searchTextColor, false);
        if (!searchQuery.isEmpty()) {
            ArabicTextRenderer.drawString("x", searchX + searchW - 10.0f, contentY + 7.5f, 0xFFF85149, false);
        }

        // Quick Filter Button (All / Active / HUD)
        float filterBtnX = contentX + contentW - 148.0f;
        String filterLabel = quickFilterMode == 1
            ? I18nManager.format("ui.filter_on")
            : (quickFilterMode == 2 ? I18nManager.format("ui.filter_hud") : I18nManager.format("ui.filter_all"));
        GL4ESRenderUtils.drawRoundedRect(filterBtnX, contentY + 3, 40.0f, 18.0f, 4.0f, quickFilterMode > 0 ? themeAccent : 0xFF21262D);
        int ftw = ArabicTextRenderer.getStringWidth(filterLabel);
        ArabicTextRenderer.drawString(filterLabel, filterBtnX + (40.0f - ftw) / 2.0f, contentY + 8.0f, quickFilterMode > 0 ? 0xFF0B0E14 : 0xFFE6EDF3, false);

        // Touch Scroll Up / Down Buttons
        float upBtnX = contentX + contentW - 104.0f;
        float dnBtnX = contentX + contentW - 81.0f;
        GL4ESRenderUtils.drawRoundedRect(upBtnX, contentY + 3, 20.0f, 18.0f, 4.0f, 0xFF21262D);
        ArabicTextRenderer.drawString("^", upBtnX + 7, contentY + 8, 0xFFE6EDF3, false);
        GL4ESRenderUtils.drawRoundedRect(dnBtnX, contentY + 3, 20.0f, 18.0f, 4.0f, 0xFF21262D);
        ArabicTextRenderer.drawString("v", dnBtnX + 7, contentY + 8, 0xFFE6EDF3, false);

        // Language switch button
        float langBtnX = contentX + contentW - 57.0f;
        GL4ESRenderUtils.drawRoundedRect(langBtnX, contentY + 3, 53.0f, 18.0f, 4.0f, themeAccent);
        ArabicTextRenderer.drawString(
            I18nManager.getCurrentLanguage().getDisplayName(),
            langBtnX + 8,
            contentY + 8,
            0xFF0B0E14,
            false
        );

        float listY = contentY + 28.0f;
        float listH = contentH - 28.0f;
        GL4ESRenderUtils.enableScissor((int) contentX, (int) listY, (int) contentW, (int) listH);

        if (selectedTab == 6) {
            renderProfilesTab(contentX, listY, contentW, themeAccent);
        } else if (selectedTab == 8) {
            renderSettingsTab(contentX, listY, contentW, themeAccent);
        } else if (selectedTab == 9) {
            renderAboutTab(contentX, listY, contentW, themeAccent);
        } else {
            renderModulesTab(contentX, listY, contentW, rtl, themeAccent);
        }

        GL4ESRenderUtils.disableScissor();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderModulesTab(float contentX, float listY, float contentW, boolean rtl, int themeAccent) {
        ModuleManager mm = NexoraClient.getInstance().getModuleManager();
        float cardY = listY + scrollOffset;

        // Live Performance & Combat Status Card on Home Tab (selectedTab == 0)
        if (selectedTab == 0 && searchQuery.isEmpty()) {
            GL4ESRenderUtils.drawBorderedRoundedRect(contentX, cardY, contentW, 44.0f, 5.0f, 1.0f, 0xFF111621, themeAccent);
            int fps = Minecraft.getDebugFPS();
            int msWhole = (int) mm.getAvgFrameTimeMs();
            int msFrac = Math.abs((int) ((mm.getAvgFrameTimeMs() - msWhole) * 10));
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
            long maxMb = rt.maxMemory() / (1024L * 1024L);

            ArabicTextRenderer.drawString(
                "FPS: " + fps + " (1% Low: " + mm.getOnePercentLowFps() + " | " + msWhole + "." + msFrac + " ms)  |  RAM: " + usedMb + "/" + maxMb + " MB",
                contentX + 8,
                cardY + 7,
                0xFF3FB950,
                true
            );
            String hitDelayStatus = mm.isHitDelayFixEnabled() ? "0t (Opt-In)" : "10t (Vanilla Fair)";
            ArabicTextRenderer.drawString(
                "CPS: " + mm.getLeftCps() + "L/" + mm.getRightCps() + "R  |  BPS: " + mm.getBlocksPerSecond() + "  |  HitDelay: " + hitDelayStatus + "  |  Mods: " + mm.getAllModules().size(),
                contentX + 8,
                cardY + 23,
                0xFFE6EDF3,
                false
            );
            cardY += 50.0f;
        }

        // Performance Lab Live Stutter & Preset Banner on Performance Tab (selectedTab == 3)
        if (selectedTab == 3 && searchQuery.isEmpty()) {
            GL4ESRenderUtils.drawBorderedRoundedRect(contentX, cardY, contentW, 44.0f, 5.0f, 1.0f, 0xFF111621, 0xFF3FB950);
            int spikeWhole = (int) mm.getMaxFrameSpikeMs();
            int spikeFrac = Math.abs((int) ((mm.getMaxFrameSpikeMs() - spikeWhole) * 10));
            String presetName = mm.getPerformancePreset() == 0 ? "Low (GL4ES)" : (mm.getPerformancePreset() == 1 ? "Balanced" : "High");
            ArabicTextRenderer.drawString(
                "Perf Lab: " + presetName + "  |  1% Low: " + mm.getOnePercentLowFps() + " FPS  |  Stutters (>33ms): " + mm.getStutterEventCount(),
                contentX + 8,
                cardY + 7,
                0xFF3FB950,
                true
            );
            ArabicTextRenderer.drawString(
                "Peak Frame Spike: " + spikeWhole + "." + spikeFrac + " ms  |  Particle Cap: " + mm.getParticleBudgetCap() + "/s  |  Corpses Cleared: " + mm.getCorpseCleanedCount(),
                contentX + 8,
                cardY + 23,
                0xFFE6EDF3,
                false
            );
            cardY += 50.0f;
        }

        List<Module> modules = mm.getAllModules();
        int shownCount = 0;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (!matchesFilterAndSearch(m, selectedTab)) {
                continue;
            }
            shownCount++;
            int cardBorder = m.isEnabled() ? ((themeAccent & 0x00FFFFFF) | 0x99000000) : 0x22FFFFFF;
            GL4ESRenderUtils.drawBorderedRoundedRect(
                contentX,
                cardY,
                contentW,
                34.0f,
                5.0f,
                1.0f,
                0xFF161B22,
                cardBorder
            );

            String title = rtl ? m.getNameAr() : m.getNameEn();
            String desc = rtl ? m.getDescriptionAr() : m.getDescriptionEn();
            if (m instanceof HudModule) {
                title = title + " [HUD]";
            } else if (m.getKeyCode() > 0) {
                title = title + " [" + Keyboard.getKeyName(m.getKeyCode()) + "]";
            }
            ArabicTextRenderer.drawString(title, contentX + 10, cardY + 6, 0xFFE6EDF3, true);
            ArabicTextRenderer.drawString(desc, contentX + 10, cardY + 19, 0xFF8B949E, false);

            // Modern Pill Toggle Switch on the right of card
            float toggleX = contentX + contentW - 42.0f;
            GL4ESRenderUtils.drawToggleSwitch(toggleX, cardY + 10.0f, 32.0f, 14.0f, m.isEnabled(), themeAccent);

            cardY += 38.0f;
        }

        if (shownCount == 0) {
            ArabicTextRenderer.drawString(
                rtl ? "لا توجد ميزات مطابقة للبحث الحالي." : "No modules match your current search filter.",
                contentX + 12,
                cardY + 12,
                0xFF8B949E,
                false
            );
        }
    }

    private void renderProfilesTab(float contentX, float listY, float contentW, int themeAccent) {
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
                isActive ? themeAccent : 0x22FFFFFF
            );
            ArabicTextRenderer.drawString("Profile: " + pName, contentX + 10, cardY + 11, 0xFFE6EDF3, true);

            float btnX = contentX + contentW - 68.0f;
            GL4ESRenderUtils.drawRoundedRect(btnX, cardY + 7, 58.0f, 18.0f, 4.0f, isActive ? themeAccent : 0xFF21262D);
            String label = isActive ? "ACTIVE" : "LOAD";
            int tw = ArabicTextRenderer.getStringWidth(label);
            ArabicTextRenderer.drawString(label, btnX + (58.0f - tw) / 2.0f, cardY + 12, isActive ? 0xFF0B0E14 : 0xFFE6EDF3, false);
            cardY += 38.0f;
        }
    }

    private void renderSettingsTab(float contentX, float listY, float contentW, int themeAccent) {
        ModuleManager mm = NexoraClient.getInstance().getModuleManager();
        float y = listY + 4.0f + scrollOffset;

        // Card 1: Client Theme Accent Palette
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 42.0f, 5.0f, 1.0f, 0xFF161B22, 0x33FFFFFF);
        ArabicTextRenderer.drawString("NEXORA Client Theme Accent Palette", contentX + 10, y + 7, 0xFFE6EDF3, true);
        String[] tNames = { "Blue", "Purple", "Emerald", "Crimson", "Amber" };
        float bx = contentX + 10;
        for (int i = 0; i < THEME_ACCENT_PRESETS.length; i++) {
            boolean sel = (mm.getClientThemeColor() & 0x00FFFFFF) == (THEME_ACCENT_PRESETS[i] & 0x00FFFFFF);
            GL4ESRenderUtils.drawBorderedRoundedRect(bx, y + 20, 52.0f, 16.0f, 3.5f, 1.2f, THEME_ACCENT_PRESETS[i], sel ? 0xFFFFFFFF : 0xFF21262D);
            ArabicTextRenderer.drawString(tNames[i], bx + 7, y + 24, 0xFF0B0E14, false);
            bx += 58.0f;
        }
        y += 48.0f;

        // Card 2: HurtCam Multiplier
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 42.0f, 5.0f, 1.0f, 0xFF161B22, 0x33FFFFFF);
        int hcPct = Math.round(mm.getHurtCamMultiplier() * 100.0f);
        ArabicTextRenderer.drawString("HurtCam Shake Intensity: " + hcPct + "%", contentX + 10, y + 7, 0xFFE6EDF3, true);
        float[] presets = { 0.0f, 0.35f, 0.70f, 1.0f };
        bx = contentX + 10;
        for (int i = 0; i < presets.length; i++) {
            boolean sel = Math.abs(mm.getHurtCamMultiplier() - presets[i]) < 0.05f;
            GL4ESRenderUtils.drawRoundedRect(bx, y + 20, 48.0f, 16.0f, 3.5f, sel ? themeAccent : 0xFF21262D);
            ArabicTextRenderer.drawString(Math.round(presets[i] * 100) + "%", bx + 14, y + 24, sel ? 0xFF0B0E14 : 0xFFE6EDF3, false);
            bx += 54.0f;
        }
        y += 48.0f;

        // Card 3: Hit Color Presets
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

        // Card 4: Performance Lab Preset & Safe Mode
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 42.0f, 5.0f, 1.0f, 0xFF161B22, 0x33FFFFFF);
        ArabicTextRenderer.drawString("Performance Lab Preset & Safe Mode", contentX + 10, y + 7, 0xFFE6EDF3, true);
        String[] perfNames = { "Low (GL4ES)", "Balanced", "High", mm.isSafeModeActive() ? "Safe: ON" : "Safe: OFF" };
        bx = contentX + 10;
        for (int i = 0; i < 4; i++) {
            boolean sel = (i < 3) ? (mm.getPerformancePreset() == i) : mm.isSafeModeActive();
            int bgCol = (i == 3 && sel) ? 0xFF3FB950 : (sel ? themeAccent : 0xFF21262D);
            GL4ESRenderUtils.drawRoundedRect(bx, y + 20, 68.0f, 16.0f, 3.5f, bgCol);
            ArabicTextRenderer.drawString(perfNames[i], bx + 6, y + 24, sel ? 0xFF0B0E14 : 0xFFE6EDF3, false);
            bx += 74.0f;
        }
        y += 48.0f;

        // Card 5: Particle Studio (Crit Multiplier & Particle Budget Cap)
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 42.0f, 5.0f, 1.0f, 0xFF161B22, 0x33FFFFFF);
        ArabicTextRenderer.drawString("Particle Studio: Crit Multiplier (" + mm.getParticleMultiplier() + "x) & Budget (" + mm.getParticleBudgetCap() + "/s)", contentX + 10, y + 7, 0xFFE6EDF3, true);
        String[] partLabels = { "Off", "1x", "2x", "3x", "Cap:50", "Cap:150", "Cap:300" };
        bx = contentX + 10;
        int[] caps = { 50, 150, 300 };
        for (int i = 0; i < partLabels.length; i++) {
            boolean sel = (i < 4) ? (mm.getParticleMultiplier() == i) : (mm.getParticleBudgetCap() == caps[i - 4]);
            float bw = (i < 4) ? 32.0f : 46.0f;
            GL4ESRenderUtils.drawRoundedRect(bx, y + 20, bw, 16.0f, 3.5f, sel ? themeAccent : 0xFF21262D);
            ArabicTextRenderer.drawString(partLabels[i], bx + 5, y + 24, sel ? 0xFF0B0E14 : 0xFFE6EDF3, false);
            bx += bw + 5.0f;
        }
        y += 48.0f;

        // Card 6: Builder Studio Build Height Limit Preset
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 42.0f, 5.0f, 1.0f, 0xFF161B22, 0x33FFFFFF);
        ArabicTextRenderer.drawString("Builder Studio Ceiling Alert: " + mm.getMaxBuildHeight() + "Y", contentX + 10, y + 7, 0xFFE6EDF3, true);
        int[] ceilPresets = { 100, 128, 180, 256 };
        String[] ceilLabels = { "BedWars 100Y", "SkyWars 128Y", "Arena 180Y", "Vanilla 256Y" };
        bx = contentX + 10;
        for (int i = 0; i < ceilPresets.length; i++) {
            boolean sel = mm.getMaxBuildHeight() == ceilPresets[i];
            GL4ESRenderUtils.drawRoundedRect(bx, y + 20, 68.0f, 16.0f, 3.5f, sel ? themeAccent : 0xFF21262D);
            ArabicTextRenderer.drawString(ceilLabels[i], bx + 5, y + 24, sel ? 0xFF0B0E14 : 0xFFE6EDF3, false);
            bx += 74.0f;
        }
        y += 48.0f;

        // Card 7: Reset All Modules & HUD to Factory Defaults
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 34.0f, 5.0f, 1.0f, 0xFF161B22, 0x44F85149);
        ArabicTextRenderer.drawString("Factory Reset All Modules & HUD Positions", contentX + 10, y + 12, 0xFFE6EDF3, true);
        float resetBtnX = contentX + contentW - 76.0f;
        GL4ESRenderUtils.drawRoundedRect(resetBtnX, y + 8, 66.0f, 18.0f, 4.0f, 0xFFF85149);
        ArabicTextRenderer.drawString("RESET ALL", resetBtnX + 8, y + 13, 0xFFFFFFFF, false);
    }

    private void renderAboutTab(float contentX, float listY, float contentW, int themeAccent) {
        ModuleManager mm = NexoraClient.getInstance().getModuleManager();
        float y = listY + 4.0f + scrollOffset;
        GL4ESRenderUtils.drawBorderedRoundedRect(contentX, y, contentW, 215.0f, 5.0f, 1.0f, 0xFF161B22, themeAccent);
        ArabicTextRenderer.drawString("NEXORA CLIENT v1.0.0 — Forge 1.8.9 (11.15.1.2318)", contentX + 10, y + 10, themeAccent, true);
        ArabicTextRenderer.drawString("Target Runtime: Java 8 Bytecode (Major 52) · PojavLauncher Android", contentX + 10, y + 26, 0xFFE6EDF3, false);
        ArabicTextRenderer.drawString("GPU Renderer: GL4ES Safe Pipeline (CCW Triangle Fan · Zero Shaders)", contentX + 10, y + 40, 0xFF3FB950, false);
        ArabicTextRenderer.drawString("1.8.9 HitDelayFix: MixinMinecraft (leftClickCounter = 0 on Miss)", contentX + 10, y + 54, 0xFF00A8FF, false);
        ArabicTextRenderer.drawString("HUD Pipeline: 3-Layer Hook (" + mm.getHudModules().size() + " HUD Widgets · " + mm.getAllModules().size() + " Total Mods)", contentX + 10, y + 68, 0xFFE6EDF3, false);
        ArabicTextRenderer.drawString("Arabic Engine: ICU4J ArabicShaping & Bidi + 512-Entry LRU Cache", contentX + 10, y + 82, 0xFFE6EDF3, false);
        ArabicTextRenderer.drawString("Reference Audit (HitDelayFix 1.1.6): Clean PvP & UI Adopted Only", contentX + 10, y + 96, 0xFF3FB950, false);
        ArabicTextRenderer.drawString("Rejected Unsafe/Unfair: JarHider, WindowsAPI, AimAssist, MicroReach", contentX + 10, y + 110, 0xFFF85149, false);
        ArabicTextRenderer.drawString("Recommended External Mods (.minecraft/mods/1.8.9/):", contentX + 10, y + 130, 0xFFA371F7, true);
        ArabicTextRenderer.drawString("1. OptiFine 1.8.9 HD U M5 (Fast Render OFF for HUD compatibility)", contentX + 10, y + 146, 0xFF8B949E, false);
        ArabicTextRenderer.drawString("2. Patcher 1.8.9 & MouseDelayFix 1.0 (Fully compatible)", contentX + 10, y + 160, 0xFF8B949E, false);
        ArabicTextRenderer.drawString("Shortcuts: [RSHIFT] Menu | [H] HUD Editor | [F] Snaplook | [C] Zoom", contentX + 10, y + 180, 0xFFD29922, true);
    }

    private boolean matchesFilterAndSearch(Module m, int tab) {
        if (quickFilterMode == 1 && !m.isEnabled()) {
            return false;
        }
        if (quickFilterMode == 2 && !(m instanceof HudModule)) {
            return false;
        }
        if (!searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            boolean matchEn = m.getNameEn().toLowerCase(Locale.ROOT).contains(q)
                || m.getDescriptionEn().toLowerCase(Locale.ROOT).contains(q)
                || m.getId().toLowerCase(Locale.ROOT).contains(q);
            boolean matchAr = m.getNameAr().contains(searchQuery) || m.getDescriptionAr().contains(searchQuery);
            if (!matchEn && !matchAr) {
                return false;
            }
        }
        if (tab == 0 || tab == 7) return true;
        if (tab == 2) return m.getCategory() == ModuleCategory.PVP || m.getCategory() == ModuleCategory.HUD;
        if (tab == 3) return m.getCategory() == ModuleCategory.PERFORMANCE;
        if (tab == 4) return m.getCategory() == ModuleCategory.VISUALS;
        if (tab == 5) return m.getCategory() == ModuleCategory.INVENTORY;
        return true;
    }

    private void scrollBy(float delta) {
        targetScrollOffset += delta;
        if (targetScrollOffset > 0.0f) {
            targetScrollOffset = 0.0f;
        }
        float minScroll = -1420.0f;
        if (targetScrollOffset < minScroll) {
            targetScrollOffset = minScroll;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchFocused = false;
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                searchFocused = false;
                return;
            }
            if (keyCode == Keyboard.KEY_BACK) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    targetScrollOffset = 0.0f;
                }
                return;
            }
            if (ChatAllowedCharacters.isAllowedCharacter(typedChar) && searchQuery.length() < 24) {
                searchQuery += typedChar;
                targetScrollOffset = 0.0f;
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollBy(wheel > 0 ? 32.0f : -32.0f);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (touchDraggingScroll && clickedMouseButton == 0) {
            int dy = mouseY - lastTouchY;
            lastTouchY = mouseY;
            scrollBy(dy * 1.3f);
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
        ModuleManager mm = NexoraClient.getInstance().getModuleManager();

        float winW = Math.min(468.0f, sw - 14.0f);
        float winH = Math.min(290.0f, sh - 14.0f);
        float winX = (sw - winW) / 2.0f;
        float winY = (sh - winH) / 2.0f;
        float sideW = 112.0f;
        float sideX = rtl ? (winX + winW - sideW) : winX;

        // 1. Check Sidebar clicks
        float navY = winY + 34.0f;
        for (int i = 0; i < NAV_KEYS.length; i++) {
            if (mouseX >= sideX + 5 && mouseX <= sideX + sideW - 5 && mouseY >= navY && mouseY <= navY + 20) {
                mm.playUiClickSound();
                if (i == 1) {
                    Minecraft.getMinecraft().displayGuiScreen(new GuiHudEditor());
                    return;
                }
                selectedTab = i;
                targetScrollOffset = 0.0f;
                scrollOffset = 0.0f;
                return;
            }
            navY += 22.0f;
        }

        float contentX = rtl ? (winX + 10.0f) : (winX + sideW + 10.0f);
        float contentW = winW - sideW - 20.0f;
        float contentY = winY + 8.0f;

        // 2. Check Top Bar controls (Search box, Quick Filter, Scroll Up/Down, Language Switch)
        float searchX = contentX + 4.0f;
        float searchW = contentW - 156.0f;
        float filterBtnX = contentX + contentW - 148.0f;
        float upBtnX = contentX + contentW - 104.0f;
        float dnBtnX = contentX + contentW - 81.0f;
        float langBtnX = contentX + contentW - 57.0f;

        if (mouseY >= contentY + 3 && mouseY <= contentY + 21) {
            if (mouseX >= searchX && mouseX <= searchX + searchW) {
                if (!searchQuery.isEmpty() && mouseX >= searchX + searchW - 14.0f) {
                    searchQuery = "";
                    targetScrollOffset = 0.0f;
                }
                searchFocused = true;
                mm.playUiClickSound();
                return;
            } else {
                searchFocused = false;
            }

            if (mouseX >= filterBtnX && mouseX <= filterBtnX + 40.0f) {
                quickFilterMode = (quickFilterMode + 1) % 3;
                targetScrollOffset = 0.0f;
                mm.playUiClickSound();
                return;
            }
            if (mouseX >= upBtnX && mouseX <= upBtnX + 20.0f) {
                scrollBy(75.0f);
                mm.playUiClickSound();
                return;
            }
            if (mouseX >= dnBtnX && mouseX <= dnBtnX + 20.0f) {
                scrollBy(-75.0f);
                mm.playUiClickSound();
                return;
            }
            if (mouseX >= langBtnX && mouseX <= langBtnX + 53.0f) {
                I18nManager.Language next = (I18nManager.getCurrentLanguage() == I18nManager.Language.AR)
                    ? I18nManager.Language.EN
                    : I18nManager.Language.AR;
                I18nManager.setCurrentLanguage(next);
                mm.playUiClickSound();
                saveActiveProfile();
                return;
            }
        } else {
            searchFocused = false;
        }

        float listY = contentY + 28.0f;

        // 3. Profiles Tab clicks
        if (selectedTab == 6) {
            ProfileManager pm = NexoraClient.getInstance().getProfileManager();
            List<String> profiles = pm.getProfiles();
            float cardY = listY + 4.0f;
            for (int i = 0; i < profiles.size(); i++) {
                if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= cardY && mouseY <= cardY + 32.0f) {
                    String chosen = profiles.get(i);
                    pm.switchProfile(chosen);
                    mm.playUiClickSound();
                    mm.pushToast("Profile Loaded", chosen, mm.getClientThemeColor());
                    return;
                }
                cardY += 38.0f;
            }
            return;
        }

        // 4. Settings Tab clicks
        if (selectedTab == 8) {
            float y = listY + 4.0f + scrollOffset;
            float bx = contentX + 10;
            // Theme accent presets
            for (int i = 0; i < THEME_ACCENT_PRESETS.length; i++) {
                if (mouseX >= bx && mouseX <= bx + 52.0f && mouseY >= y + 20 && mouseY <= y + 36) {
                    mm.setClientThemeColor(THEME_ACCENT_PRESETS[i]);
                    mm.playUiClickSound();
                    saveActiveProfile();
                    return;
                }
                bx += 58.0f;
            }
            y += 48.0f;

            // HurtCam presets
            float[] presets = { 0.0f, 0.35f, 0.70f, 1.0f };
            bx = contentX + 10;
            for (int i = 0; i < presets.length; i++) {
                if (mouseX >= bx && mouseX <= bx + 48.0f && mouseY >= y + 20 && mouseY <= y + 36) {
                    mm.setHurtCamMultiplier(presets[i]);
                    mm.playUiClickSound();
                    saveActiveProfile();
                    return;
                }
                bx += 54.0f;
            }
            y += 48.0f;

            // Hit color presets
            bx = contentX + 10;
            for (int i = 0; i < HIT_COLOR_PRESETS.length; i++) {
                if (mouseX >= bx && mouseX <= bx + 46.0f && mouseY >= y + 20 && mouseY <= y + 36) {
                    mm.setHitOverlayColor(HIT_COLOR_PRESETS[i]);
                    mm.playUiClickSound();
                    saveActiveProfile();
                    return;
                }
                bx += 52.0f;
            }
            y += 48.0f;

            // Card 4: Performance Lab Preset & Safe Mode
            bx = contentX + 10;
            for (int i = 0; i < 4; i++) {
                if (mouseX >= bx && mouseX <= bx + 68.0f && mouseY >= y + 20 && mouseY <= y + 36) {
                    if (i < 3) {
                        mm.applyPerformancePreset(i);
                    } else {
                        mm.setSafeModeActive(!mm.isSafeModeActive());
                    }
                    mm.playUiClickSound();
                    saveActiveProfile();
                    return;
                }
                bx += 74.0f;
            }
            y += 48.0f;

            // Card 5: Particle Studio
            bx = contentX + 10;
            int[] caps = { 50, 150, 300 };
            for (int i = 0; i < 7; i++) {
                float bw = (i < 4) ? 32.0f : 46.0f;
                if (mouseX >= bx && mouseX <= bx + bw && mouseY >= y + 20 && mouseY <= y + 36) {
                    if (i < 4) {
                        mm.setParticleMultiplier(i);
                    } else {
                        mm.setParticleBudgetCap(caps[i - 4]);
                    }
                    mm.playUiClickSound();
                    saveActiveProfile();
                    return;
                }
                bx += bw + 5.0f;
            }
            y += 48.0f;

            // Card 6: Builder Ceiling Preset
            int[] ceilPresets = { 100, 128, 180, 256 };
            bx = contentX + 10;
            for (int i = 0; i < ceilPresets.length; i++) {
                if (mouseX >= bx && mouseX <= bx + 68.0f && mouseY >= y + 20 && mouseY <= y + 36) {
                    mm.setMaxBuildHeight(ceilPresets[i]);
                    mm.playUiClickSound();
                    saveActiveProfile();
                    return;
                }
                bx += 74.0f;
            }
            y += 48.0f;

            // Card 7: Factory Reset button
            float resetBtnX = contentX + contentW - 76.0f;
            if (mouseX >= resetBtnX && mouseX <= resetBtnX + 66.0f && mouseY >= y + 8 && mouseY <= y + 26) {
                for (Module m : mm.getAllModules()) {
                    m.resetToDefault();
                }
                mm.playUiClickSound();
                mm.pushToast("Factory Reset", "Restored all " + mm.getAllModules().size() + " modules", 0xFFF85149);
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
        if ((selectedTab == 0 || selectedTab == 3) && searchQuery.isEmpty()) {
            cardY += 50.0f;
        }
        List<Module> modules = mm.getAllModules();
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (!matchesFilterAndSearch(m, selectedTab)) continue;
            float toggleX = contentX + contentW - 46.0f;
            if (mouseX >= toggleX && mouseX <= contentX + contentW - 4.0f && mouseY >= cardY + 6 && mouseY <= cardY + 28) {
                m.toggle();
                mm.playUiClickSound();
                mm.pushModuleToggleToast(m);
                saveActiveProfile();
                return;
            }
            cardY += 38.0f;
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
