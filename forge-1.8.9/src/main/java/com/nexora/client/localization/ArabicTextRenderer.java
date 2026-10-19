package com.nexora.client.localization;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NEXORA CLIENT — Native Arabic RTL & Shaping Engine for Minecraft 1.8.9.
 * Uses ICU4J (com.ibm.icu.text.ArabicShaping & Bidi) bundled with Minecraft 1.8.9
 * to connect Arabic glyphs properly while keeping LTR numbers (FPS, Ping, Coordinates) intact.
 * Includes an LRU cache to avoid per-frame object allocations in the Render Loop.
 */
public final class ArabicTextRenderer {

    private static final int MAX_CACHE_SIZE = 512;

    private static final Map<String, String> SHAPED_CACHE = new LinkedHashMap<String, String>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private ArabicTextRenderer() {
    }

    public static boolean containsArabic(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0x0750 && c <= 0x077F) || (c >= 0xFB50 && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    public static String shapeArabicText(String input) {
        if (input == null || input.isEmpty() || !containsArabic(input)) {
            return input;
        }
        synchronized (SHAPED_CACHE) {
            String cached = SHAPED_CACHE.get(input);
            if (cached != null) {
                return cached;
            }
        }

        String result = input;
        try {
            ArabicShaping shaper = new ArabicShaping(
                ArabicShaping.LETTERS_SHAPE | ArabicShaping.TEXT_DIRECTION_LOGICAL
            );
            String shaped = shaper.shape(input);
            Bidi bidi = new Bidi(
                shaped,
                Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
            );
            result = bidi.writeReordered(Bidi.DO_MIRRORING);
        } catch (ArabicShapingException e) {
            result = input;
        }

        synchronized (SHAPED_CACHE) {
            SHAPED_CACHE.put(input, result);
        }
        return result;
    }

    public static void drawString(String text, float x, float y, int color, boolean shadow) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        if (fr == null || text == null) {
            return;
        }
        String processed = shapeArabicText(text);
        fr.drawString(processed, x, y, color, shadow);
    }

    public static void drawAlignedString(String text, float x, float y, int width, int color, boolean rightAligned) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        if (fr == null || text == null) {
            return;
        }
        String processed = shapeArabicText(text);
        if (rightAligned) {
            int textWidth = fr.getStringWidth(processed);
            fr.drawStringWithShadow(processed, x + width - textWidth, y, color);
        } else {
            fr.drawStringWithShadow(processed, x, y, color);
        }
    }

    public static int getStringWidth(String text) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        if (fr == null || text == null) {
            return 0;
        }
        return fr.getStringWidth(shapeArabicText(text));
    }

    public static void clearCache() {
        synchronized (SHAPED_CACHE) {
            SHAPED_CACHE.clear();
        }
    }
}
