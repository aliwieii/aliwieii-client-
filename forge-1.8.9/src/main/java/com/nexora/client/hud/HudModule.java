package com.nexora.client.hud;

import com.nexora.client.localization.ArabicTextRenderer;
import com.nexora.client.modules.Module;
import com.nexora.client.modules.ModuleCategory;
import com.nexora.client.utils.GL4ESRenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * Base class for draggable, resizable, and lockable HUD elements in NEXORA CLIENT.
 * Enforces screen bounds clamping and per-profile coordinate persistence.
 */
public abstract class HudModule extends Module {

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
        clampToScreen(sr);

        GL11.glPushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);

        if (showBackground) {
            int alpha = Math.min(255, Math.max(20, (int) (opacity * 255.0f)));
            int bgWithOpacity = (backgroundColor & 0x00FFFFFF) | (alpha << 24);
            int borderCol = editMode ? (locked ? 0xFFF85149 : accentColor) : 0x33FFFFFF;
            GL4ESRenderUtils.drawBorderedRoundedRect(0, 0, width, height, 4.0f, 1.0f, bgWithOpacity, borderCol);
        }

        renderContent(editMode);

        GL11.glPopMatrix();
    }

    protected abstract void renderContent(boolean editMode);

    protected void drawCenteredText(String text, float centerX, float topY, int color) {
        int textWidth = ArabicTextRenderer.getStringWidth(text);
        ArabicTextRenderer.drawString(text, centerX - (textWidth / 2.0f), topY, color, true);
    }

    public void clampToScreen(ScaledResolution sr) {
        float scaledW = width * scale;
        float scaledH = height * scale;
        float maxW = sr.getScaledWidth() - scaledW;
        float maxH = sr.getScaledHeight() - scaledH;
        if (x < 2.0f) x = 2.0f;
        if (y < 2.0f) y = 2.0f;
        if (x > maxW - 2.0f) x = Math.max(2.0f, maxW - 2.0f);
        if (y > maxH - 2.0f) y = Math.max(2.0f, maxH - 2.0f);
    }

    @Override
    public void resetToDefault() {
        super.resetToDefault();
        this.x = defaultX;
        this.y = defaultY;
        this.scale = 1.0f;
        this.opacity = 0.85f;
        this.accentColor = 0xFF00A8FF;
        this.locked = false;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        float scaledW = width * scale;
        float scaledH = height * scale;
        return mouseX >= x && mouseX <= x + scaledW && mouseY >= y && mouseY <= y + scaledH;
    }

    public float getX() { return x; }
    public void setX(float x) { if (!locked) this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { if (!locked) this.y = y; }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = Math.max(0.5f, Math.min(2.25f, scale)); }

    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { this.opacity = Math.max(0.1f, Math.min(1.0f, opacity)); }

    public int getAccentColor() { return accentColor; }
    public void setAccentColor(int accentColor) { this.accentColor = accentColor; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public boolean isShowBackground() { return showBackground; }
    public void setShowBackground(boolean showBackground) { this.showBackground = showBackground; }
}
