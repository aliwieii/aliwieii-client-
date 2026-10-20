package com.nexora.client.modules;

public enum ModuleCategory {
    HUD("HUD & Info", "عناصر الشاشة والمعلومات"),
    PVP("Movement & PvP", "الحركة وأدوات القتال"),
    VISUALS("Visuals & Effects", "الرسوميات والمؤثرات"),
    INVENTORY("Inventory & Chat", "المخزون والدردشة"),
    PERFORMANCE("Performance", "الأداء والتحسين");

    private final String titleEn;
    private final String titleAr;

    ModuleCategory(String titleEn, String titleAr) {
        this.titleEn = titleEn;
        this.titleAr = titleAr;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public String getTitleAr() {
        return titleAr;
    }
}
