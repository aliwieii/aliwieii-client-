package com.nexora.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * NEXORA CLIENT — GL4ES & PojavLauncher Safe Rendering Engine (Adreno 710 Tested Pattern).
 * Avoids custom GLSL shaders that cause black screens or frame drops on mobile GL4ES.
 * Uses native OpenGL ES 2.0 primitives (GL_TRIANGLE_FAN, GL_TRIANGLES, GL_LINE_LOOP)
 * with Counter-Clockwise (CCW) vertex winding and strict GlStateManager restoration.
 */
public final class GL4ESRenderUtils {

    private static final double[] SIN_LUT = new double[91];
    private static final double[] COS_LUT = new double[91];

    static {
        for (int i = 0; i <= 90; i++) {
            double rad = Math.toRadians(i);
            SIN_LUT[i] = Math.sin(rad);
            COS_LUT[i] = Math.cos(rad);
        }
    }

    private GL4ESRenderUtils() {
    }

    public static float lerp(float current, float target, float factor) {
        if (Math.abs(target - current) < 0.001f) {
            return target;
        }
        return current + (target - current) * Math.max(0.0f, Math.min(1.0f, factor));
    }

    public static void resetGlState() {
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left > right) {
            float tmp = left;
            left = right;
            right = tmp;
        }
        if (top > bottom) {
            float tmp = top;
            top = bottom;
            bottom = tmp;
        }

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(r, g, b, a);

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws a rounded rectangle using GL_TRIANGLE_FAN (native OpenGL ES 2.0 primitive)
     * with continuous Counter-Clockwise (CCW) vertex winding so it is never back-face culled.
     */
    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        if (radius <= 0.5f) {
            drawRect(x, y, x + width, y + height, color);
            return;
        }
        float x2 = x + width;
        float y2 = y + height;
        float r = Math.min(radius, Math.min(width, height) / 2.0f);

        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);

        // Center anchor vertex for triangle fan
        wr.pos(x + width / 2.0f, y + height / 2.0f, 0.0D).endVertex();

        int step = 15;
        // 1. Top-Left corner: (x+r, y) -> (x, y+r)
        for (int i = 90; i >= 0; i -= step) {
            wr.pos(x + r - COS_LUT[i] * r, y + r - SIN_LUT[i] * r, 0.0D).endVertex();
        }
        // 2. Bottom-Left corner: (x, y2-r) -> (x+r, y2)
        for (int i = 0; i <= 90; i += step) {
            wr.pos(x + r - COS_LUT[i] * r, y2 - r + SIN_LUT[i] * r, 0.0D).endVertex();
        }
        // 3. Bottom-Right corner: (x2-r, y2) -> (x2, y2-r)
        for (int i = 90; i >= 0; i -= step) {
            wr.pos(x2 - r + COS_LUT[i] * r, y2 - r + SIN_LUT[i] * r, 0.0D).endVertex();
        }
        // 4. Top-Right corner: (x2, y+r) -> (x2-r, y)
        for (int i = 0; i <= 90; i += step) {
            wr.pos(x2 - r + COS_LUT[i] * r, y + r - SIN_LUT[i] * r, 0.0D).endVertex();
        }
        // Close fan back to first perimeter vertex (x+r, y)
        wr.pos(x + r, y, 0.0D).endVertex();

        tessellator.draw();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void drawBorderedRoundedRect(float x, float y, float width, float height, float radius, float borderWidth, int bgColor, int borderColor) {
        drawRoundedRect(x, y, width, height, radius, bgColor);
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }

        float x2 = x + width;
        float y2 = y + height;
        float r = Math.min(radius, Math.min(width, height) / 2.0f);

        float alpha = (float) (borderColor >> 24 & 255) / 255.0F;
        float red = (float) (borderColor >> 16 & 255) / 255.0F;
        float green = (float) (borderColor >> 8 & 255) / 255.0F;
        float blue = (float) (borderColor & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        GL11.glLineWidth(Math.max(1.0f, borderWidth));

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);

        int step = 15;
        for (int i = 90; i >= 0; i -= step) {
            wr.pos(x + r - COS_LUT[i] * r, y + r - SIN_LUT[i] * r, 0.0D).endVertex();
        }
        for (int i = 0; i <= 90; i += step) {
            wr.pos(x + r - COS_LUT[i] * r, y2 - r + SIN_LUT[i] * r, 0.0D).endVertex();
        }
        for (int i = 90; i >= 0; i -= step) {
            wr.pos(x2 - r + COS_LUT[i] * r, y2 - r + SIN_LUT[i] * r, 0.0D).endVertex();
        }
        for (int i = 0; i <= 90; i += step) {
            wr.pos(x2 - r + COS_LUT[i] * r, y + r - SIN_LUT[i] * r, 0.0D).endVertex();
        }

        tessellator.draw();
        GL11.glLineWidth(1.0F);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    /**
     * Draws a modern pill-style toggle switch with a sliding knob (GL4ES safe).
     */
    public static void drawToggleSwitch(float x, float y, float width, float height, boolean enabled, int accentColor) {
        float radius = height / 2.0f;
        int trackColor = enabled ? accentColor : 0xFF21262D;
        int borderColor = enabled ? 0x66FFFFFF : 0x338B949E;
        drawBorderedRoundedRect(x, y, width, height, radius, 1.0f, trackColor, borderColor);

        float knobSize = height - 4.0f;
        float knobX = enabled ? (x + width - knobSize - 2.0f) : (x + 2.0f);
        float knobY = y + 2.0f;
        int knobColor = enabled ? 0xFF0B0E14 : 0xFFE6EDF3;
        drawRoundedRect(knobX, knobY, knobSize, knobSize, knobSize / 2.0f, knobColor);
    }

    /**
     * Draws a 2D line segment using GL_LINES (for crosshair hit markers & indicators).
     */
    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(Math.max(1.0f, lineWidth));

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(x1, y1, 0.0D).endVertex();
        wr.pos(x2, y2, 0.0D).endVertex();
        tessellator.draw();

        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    /**
     * Renders the geometric NEXORA emblem using pure GL11 vertices (zero texture memory overhead).
     */
    public static void drawNexoraEmblem(float centerX, float centerY, float size, int primaryColor, int secondaryColor) {
        float half = size / 2.0f;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        float r1 = (float) (primaryColor >> 16 & 255) / 255.0F;
        float g1 = (float) (primaryColor >> 8 & 255) / 255.0F;
        float b1 = (float) (primaryColor & 255) / 255.0F;
        float a1 = (float) (primaryColor >> 24 & 255) / 255.0F;

        GlStateManager.color(r1, g1, b1, a1);
        GL11.glLineWidth(2.0F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            wr.pos(centerX + Math.cos(angle) * half, centerY + Math.sin(angle) * half, 0.0D).endVertex();
        }
        tessellator.draw();

        float r2 = (float) (secondaryColor >> 16 & 255) / 255.0F;
        float g2 = (float) (secondaryColor >> 8 & 255) / 255.0F;
        float b2 = (float) (secondaryColor & 255) / 255.0F;
        float a2 = (float) (secondaryColor >> 24 & 255) / 255.0F;
        GlStateManager.color(r2, g2, b2, a2);

        float inner = half * 0.55f;
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        wr.pos(centerX - inner, centerY + inner, 0.0D).endVertex();
        wr.pos(centerX - inner, centerY - inner, 0.0D).endVertex();
        wr.pos(centerX + inner, centerY + inner, 0.0D).endVertex();
        wr.pos(centerX + inner, centerY - inner, 0.0D).endVertex();
        tessellator.draw();

        GL11.glLineWidth(1.0F);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void enableScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int scissorX = x * scale;
        int scissorY = (sr.getScaledHeight() - (y + height)) * scale;
        int scissorW = Math.max(0, width * scale);
        int scissorH = Math.max(0, height * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
    }

    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
