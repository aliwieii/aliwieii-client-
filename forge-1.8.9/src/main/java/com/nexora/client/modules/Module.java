package com.nexora.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

/**
 * Base Module class in NEXORA CLIENT.
 * Manages lifecycle, event registration, keybinding, and resource cleanup.
 */
public abstract class Module {

    protected final Minecraft mc = Minecraft.getMinecraft();
    private final String id;
    private final String nameEn;
    private final String nameAr;
    private final String descriptionEn;
    private final String descriptionAr;
    private final ModuleCategory category;
    private final boolean defaultEnabled;

    private boolean enabled;
    private int keyCode;

    protected Module(
        String id,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        ModuleCategory category,
        boolean defaultEnabled,
        int defaultKeyCode
    ) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameAr = nameAr;
        this.descriptionEn = descriptionEn;
        this.descriptionAr = descriptionAr;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
        this.enabled = defaultEnabled;
        this.keyCode = defaultKeyCode;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    public void resetToDefault() {
        setEnabled(defaultEnabled);
    }

    public String getId() {
        return id;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameAr() {
        return nameAr;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public String getDescriptionAr() {
        return descriptionAr;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }
}
