package com.nexora.client.localization;

import java.util.HashMap;
import java.util.Map;

/**
 * NEXORA CLIENT — Bilingual Localization Registry (Arabic RTL & English LTR).
 * Instant language switching without restarting Minecraft.
 */
public final class I18nManager {

    public enum Language {
        AR("ar", "العربية", true),
        EN("en", "English", false);

        private final String code;
        private final String displayName;
        private final boolean rtl;

        Language(String code, String displayName, boolean rtl) {
            this.code = code;
            this.displayName = displayName;
            this.rtl = rtl;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isRtl() {
            return rtl;
        }
    }

    private static Language currentLanguage = Language.AR;
    private static final Map<String, String> AR_MAP = new HashMap<String, String>();
    private static final Map<String, String> EN_MAP = new HashMap<String, String>();

    static {
        register("nav.home", "الرئيسية", "Home");
        register("nav.hud_editor", "محرر الواجهة", "HUD Editor");
        register("nav.pvp", "أدوات القتال PvP", "PvP");
        register("nav.performance", "الأداء", "Performance");
        register("nav.visuals", "الرسوميات", "Visuals");
        register("nav.inventory", "المخزون", "Inventory");
        register("nav.profiles", "ملفات الإعدادات", "Profiles");
        register("nav.mods", "المودات", "Mods");
        register("nav.settings", "الإعدادات", "Settings");
        register("nav.about", "حول NEXORA", "About NEXORA");

        register("ui.search_placeholder", "ابحث عن ميزة أو أداة...", "Search modules or features...");
        register("ui.enabled", "مفعل", "Enabled");
        register("ui.disabled", "معطل", "Disabled");
        register("ui.reset_defaults", "استعادة الافتراضي", "Reset Defaults");
        register("ui.open_hud_editor", "فتح محرر HUD", "Open HUD Editor");
        register("ui.save_changes", "حفظ التعديلات", "Save Changes");
        register("ui.grid_toggle", "إظهار الشبكة", "Toggle Grid");
        register("ui.auto_arrange", "ترتيب تلقائي", "Auto Arrange");
        register("ui.language", "اللغة", "Language");
        register("ui.profile_active", "الملف النشط", "Active Profile");
    }

    private I18nManager() {
    }

    private static void register(String key, String ar, String en) {
        AR_MAP.put(key, ar);
        EN_MAP.put(key, en);
    }

    public static String format(String key) {
        Map<String, String> active = (currentLanguage == Language.AR) ? AR_MAP : EN_MAP;
        String val = active.get(key);
        return val != null ? val : key;
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setCurrentLanguage(Language language) {
        if (language != null && language != currentLanguage) {
            currentLanguage = language;
            ArabicTextRenderer.clearCache();
        }
    }

    public static boolean isRtl() {
        return currentLanguage.isRtl();
    }
}
