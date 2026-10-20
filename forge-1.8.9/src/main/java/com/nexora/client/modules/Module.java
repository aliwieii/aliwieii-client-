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
    private final int defaultKeyCode;

    private boolean enabled;
    private boolean registeredOnBus = false;
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
        this.defaultKeyCode = defaultKeyCode;
        this.enabled = false;
        this.keyCode = defaultKeyCode;
        setEnabled(defaultEnabled);
    }

    public final void setEnabled(boolean targetEnabled) {
        if (this.enabled == targetEnabled && (targetEnabled == this.registeredOnBus)) {
            return;
        }
        this.enabled = targetEnabled;
        try {
            if (targetEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (Throwable ignored) {
        }
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    protected boolean usesForgeEventBus() {
        return false;
    }

    protected void onEnable() {
        if (usesForgeEventBus() && !registeredOnBus) {
            MinecraftForge.EVENT_BUS.register(this);
            registeredOnBus = true;
        }
    }

    protected void onDisable() {
        if (registeredOnBus) {
            MinecraftForge.EVENT_BUS.unregister(this);
            registeredOnBus = false;
        }
    }

    public void resetToDefault() {
        this.keyCode = defaultKeyCode;
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

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }
}
