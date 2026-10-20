package com.nexora.client.hud;

import com.nexora.client.localization.ArabicTextRenderer;
import com.nexora.client.modules.Module;
import com.nexora.client.modules.ModuleCategory;
import com.nexora.client.utils.GL4ESRenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Base class for draggable, resizable, and lockable HUD elements in NEXORA CLIENT.
 * Enforces screen bounds clamping, NaN/corruption recovery, and OpenGL state isolation.
 */
public abstract class HudModule extends Module {

    private static final int[] ACCENT_PALETTE = {
        0xFF00A8FF, // Electric Blue
        0xFFA371F7, // Cyber Purple
        0xFF3FB950, // Emerald Green
        0xFFD29922, // Amber Gold
        0xFFF85149, // Crimson Red
        0xFFE6EDF3  // Crisp Ice White
    };

    private float x;
    private float y;
    private float width;
    private float height;
    private float scale = 1.0f;
    private float opacity = 0.85f;
    private int accentColor = 0xFF00A8FF;
    private int backgroundColor = 0xB0111621;
    private boolean locked = false;
    private boolean showBackground = true;

    private final float defaultX;
    private final float defaultY;

    protected HudModule(
        String id,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        boolean defaultEnabled,
        float defaultX,
        float defaultY,
        float width,
        float height
    ) {
        super(id, nameEn, nameAr, descriptionEn, descriptionAr, ModuleCategory.HUD, defaultEnabled, 0);
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.x = defaultX;
        this.y = defaultY;
        this.width = width;
        this.height = height;
    }

    public final void renderHud(ScaledResolution sr, boolean editMode) {
        if (!isEnabled() && !editMode) {
            return;
        }
        sanitizeAndClamp(sr);

        GlStateManager.pushMatrix();
        try {
            GL4ESRenderUtils.resetGlState();
            GlStateManager.translate(x, y, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);

            if (showBackground || editMode) {
                int alpha = Math.min(255, Math.max(25, (int) (opacity * 255.0f)));
                if (!isEnabled() && editMode) {
                    alpha = 70;
                }
                int bgWithOpacity = (backgroundColor & 0x00FFFFFF) | (alpha << 24);
                int borderCol = editMode
                    ? (locked ? 0xFFF85149 : (isEnabled() ? accentColor : 0x668B949E))
                    : (showBackground ? 0x38FFFFFF : 0x00000000);

                if (showBackground) {
                    GL4ESRenderUtils.drawBorderedRoundedRect(0, 0, width, height, 4.0f, 1.0f, bgWithOpacity, borderCol);
                } else if (editMode) {
                    GL4ESRenderUtils.drawBorderedRoundedRect(0, 0, width, height, 4.0f, 1.0f, 0x33111621, borderCol);
                }
            }

            renderContent(editMode);
        } catch (Throwable ignored) {
        } finally {
            GL4ESRenderUtils.resetGlState();
            GlStateManager.popMatrix();
        }
    }

    protected abstract void renderContent(boolean editMode);

    protected void drawCenteredText(String text, float centerX, float topY, int color) {
        int textWidth = ArabicTextRenderer.getStringWidth(text);
        ArabicTextRenderer.drawString(text, centerX - (textWidth / 2.0f), topY, color, true);
    }

    public void sanitizeAndClamp(ScaledResolution sr) {
        if (Float.isNaN(x) || Float.isInfinite(x) || x < -500.0f || x > 10000.0f) {
            x = defaultX;
        }
        if (Float.isNaN(y) || Float.isInfinite(y) || y < -500.0f || y > 10000.0f) {
            y = defaultY;
        }
        if (Float.isNaN(scale) || Float.isInfinite(scale) || scale < 0.5f || scale > 2.25f) {
            scale = 1.0f;
        }
        if (Float.isNaN(opacity) || Float.isInfinite(opacity) || opacity < 0.1f || opacity > 1.0f) {
            opacity = 0.85f;
        }
        clampToScreen(sr);
    }

    public void clampToScreen(ScaledResolution sr) {
        if (sr == null) {
            return;
        }
        float scaledW = width * scale;
        float scaledH = height * scale;
        float maxW = sr.getScaledWidth() - scaledW;
        float maxH = sr.getScaledHeight() - scaledH;
        if (x < 2.0f) x = 2.0f;
        if (y < 2.0f) y = 2.0f;
        if (x > maxW - 2.0f) x = Math.max(2.0f, maxW - 2.0f);
        if (y > maxH - 2.0f) y = Math.max(2.0f, maxH - 2.0f);
    }

    public void cycleAccentColor() {
        for (int i = 0; i < ACCENT_PALETTE.length; i++) {
            if (ACCENT_PALETTE[i] == accentColor) {
                accentColor = ACCENT_PALETTE[(i + 1) % ACCENT_PALETTE.length];
                return;
            }
        }
        accentColor = ACCENT_PALETTE[0];
    }

    @Override
    public void resetToDefault() {
        super.resetToDefault();
        this.locked = false;
        this.x = defaultX;
        this.y = defaultY;
        this.scale = 1.0f;
        this.opacity = 0.85f;
        this.accentColor = 0xFF00A8FF;
        this.showBackground = true;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        float scaledW = width * scale;
        float scaledH = height * scale;
        return mouseX >= x && mouseX <= x + scaledW && mouseY >= y && mouseY <= y + scaledH;
    }

    public float getDefaultX() { return defaultX; }
    public float getDefaultY() { return defaultY; }

    public float getX() { return x; }
    public void setX(float x) {
        if (!locked && !Float.isNaN(x) && !Float.isInfinite(x)) {
            this.x = x;
        }
    }

    public float getY() { return y; }
    public void setY(float y) {
        if (!locked && !Float.isNaN(y) && !Float.isInfinite(y)) {
            this.y = y;
        }
    }

    public void forcePosition(float x, float y) {
        if (!Float.isNaN(x) && !Float.isInfinite(x)) this.x = x;
        if (!Float.isNaN(y) && !Float.isInfinite(y)) this.y = y;
    }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = Math.max(24.0f, width); }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = Math.max(14.0f, height); }

    public float getScale() { return scale; }
    public void setScale(float scale) {
        if (!Float.isNaN(scale) && !Float.isInfinite(scale)) {
            this.scale = Math.max(0.5f, Math.min(2.25f, scale));
        }
    }

    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) {
        if (!Float.isNaN(opacity) && !Float.isInfinite(opacity)) {
            this.opacity = Math.max(0.1f, Math.min(1.0f, opacity));
        }
    }

    public int getAccentColor() { return accentColor; }
    public void setAccentColor(int accentColor) { this.accentColor = accentColor; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public boolean isShowBackground() { return showBackground; }
    public void setShowBackground(boolean showBackground) { this.showBackground = showBackground; }
}
