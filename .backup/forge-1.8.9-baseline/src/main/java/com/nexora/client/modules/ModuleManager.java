package com.nexora.client.modules;

import com.nexora.client.gui.GuiHudEditor;
import com.nexora.client.hud.HudModule;
import com.nexora.client.localization.ArabicTextRenderer;
import com.nexora.client.localization.I18nManager;
import com.nexora.client.utils.GL4ESRenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NEXORA CLIENT — Central Module & HUD Registry (42 Core Modules, 20 HUD Widgets).
 * Implements zero-allocation render loops, 3-layer HUD rendering guarantee for Forge 1.8.9
 * (RenderGameOverlayEvent.Post + MixinGuiIngame + RenderTickEvent safety net),
 * 1.8.9 HitDelayFix integration, In-Game Toast Notifications, and hardware-friendly telemetry
 * for PojavLauncher (Snapdragon 6 Gen 1 / Adreno 710).
 */
public final class ModuleManager {

    private static final String[] CARDINAL_DIRS = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };
    private static final ResourceLocation BUTTON_CLICK_SOUND = new ResourceLocation("gui.button.press");

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<String, Module> modules = new LinkedHashMap<String, Module>();
    private final List<HudModule> hudModules = new ArrayList<HudModule>();

    // Frame deduplication guard so HUD renders once and only once per frame
    private long currentRenderFrameId = 0L;
    private long lastHudRenderedFrameId = -1L;

    // Real frame-time ring buffer (60 frames, primitive float array)
    private final float[] frameTimesMs = new float[60];
    private int frameTimeHead = 0;
    private int frameTimeCount = 0;
    private long lastFrameNano = System.nanoTime();
    private float currentFrameTimeMs = 16.6f;
    private float avgFrameTimeMs = 16.6f;

    // Primitive ring buffers for CPS (zero boxing in render loop)
    private final long[] leftClicks = new long[128];
    private int leftClickHead = 0;
    private int leftClickCount = 0;
    private long lastLeftClickRecordedMs = 0L;
    private boolean prevAttackKeyDown = false;

    private final long[] rightClicks = new long[128];
    private int rightClickHead = 0;
    private int rightClickCount = 0;
    private long lastRightClickRecordedMs = 0L;
    private boolean prevUseKeyDown = false;

    // Combat & Session state
    private int currentCombo = 0;
    private int maxSessionCombo = 0;
    private int totalSessionHits = 0;
    private int peakSessionCps = 0;
    private int corpseCleanedCount = 0;
    private final long sessionStartMs = System.currentTimeMillis();
    private long lastHitTimestamp = 0L;
    private float lastReachDistance = 0.0f;
    private EntityLivingBase lastTargetEntity = null;
    private long lastEnderPearlUseMs = 0L;
    private long lastGappleUseMs = 0L;
    private long lastAutoGgSentMs = 0L;

    // Throttled strings (updated every 200ms off-frame-hotpath)
    private long lastTelemetryUpdateMs = 0L;
    private String cachedFpsText = "FPS: 60 (16.6 ms)";
    private String cachedCoordsText = "X: 0  Y: 64  Z: 0 [N]";
    private String cachedDirectionText = "N | 0 deg";
    private String cachedPingText = "Ping: 0 ms";
    private String cachedMemText = "RAM: 35% (358MB)";
    private String cachedClockText = "12:00:00";
    private String cachedServerText = "Singleplayer";
    private String cachedPackText = "Default 1.8.9";
    private String cachedResourceText = "Fe: 0 · Au: 0 · Dia: 0 · Em: 0";
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm:ss");

    // Movement, Camera & Visual state flags read by Mixins & Forge Events
    private boolean sprintToggled = true;
    private boolean sneakToggled = false;
    private boolean freelookActive = false;
    private boolean snaplookActive = false;
    private boolean zoomActive = false;
    private float freelookYaw = 0.0f;
    private float freelookPitch = 0.0f;
    private int savedThirdPersonView = 0;
    private int savedSnaplookView = 0;
    private float hurtCamMultiplier = 0.35f;
    private int hitOverlayColor = 0x66FF2A2A;
    private int clientThemeColor = 0xFF00A8FF;
    private float savedGamma = 1.0f;
    private int savedParticleSetting = 0;

    // Lightweight In-Game Toast Notification State (Zero-Allocation Overlay)
    private String toastTitle = "";
    private String toastSubtitle = "";
    private int toastAccentColor = 0xFF00A8FF;
    private long toastStartMs = 0L;
    private long toastExpireMs = 0L;

    public void init() {
        // ====================================================================
        // 1. HUD MODULES (20 Draggable, Resizable, Preview-Ready Widgets)
        // ====================================================================

        // 1. FPS & Frame-Time Monitor HUD
        register(new HudModule("fps", "FPS & FrameTime", "عداد الإطارات وزمن الإطار", "Displays measured FPS and real frame time in ms", "يعرض معدل الإطارات الفعلي وزمن الإطار بالملي ثانية", true, 8, 8, 108, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedFpsText, getWidth() / 2.0f, 7.0f, 0xFFE6EDF3);
            }
        });

        // 2. CPS Counter HUD
        register(new HudModule("cps", "CPS Counter", "عداد النقرات CPS", "Left and right clicks per second using a zero-GC ring buffer", "يعرض نقرات اليسار واليمين في الثانية لدعم الماوس ولمس PojavLauncher", true, 8, 34, 86, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                int lmb = getLeftCps();
                int rmb = getRightCps();
                if (lmb > peakSessionCps) {
                    peakSessionCps = lmb;
                }
                drawCenteredText(lmb + " | " + rmb + " CPS", getWidth() / 2.0f, 7.0f, getAccentColor());
            }
        });

        // 3. Keystrokes HUD
        register(new HudModule("keystrokes", "Keystrokes", "عرض أزرار الحركة", "Visualizes WASD, Space, and mouse buttons with live CPS", "يعرض أزرار الحركة المضغوطة والنقرات بشكل مباشر", true, 8, 60, 76, 94) {
            @Override
            protected void renderContent(boolean editMode) {
                boolean w = mc.gameSettings != null && mc.gameSettings.keyBindForward.isKeyDown();
                boolean a = mc.gameSettings != null && mc.gameSettings.keyBindLeft.isKeyDown();
                boolean s = mc.gameSettings != null && mc.gameSettings.keyBindBack.isKeyDown();
                boolean d = mc.gameSettings != null && mc.gameSettings.keyBindRight.isKeyDown();
                boolean jump = mc.gameSettings != null && mc.gameSettings.keyBindJump.isKeyDown();
                boolean lmb = mc.gameSettings != null && (mc.gameSettings.keyBindAttack.isKeyDown() || Mouse.isButtonDown(0));
                boolean rmb = mc.gameSettings != null && (mc.gameSettings.keyBindUseItem.isKeyDown() || Mouse.isButtonDown(1));

                drawKeyBox("W", 26, 2, 24, 20, w);
                drawKeyBox("A", 2, 24, 22, 20, a);
                drawKeyBox("S", 26, 24, 24, 20, s);
                drawKeyBox("D", 52, 24, 22, 20, d);
                drawKeyBox("LMB", 2, 46, 35, 20, lmb);
                drawKeyBox("RMB", 39, 46, 35, 20, rmb);
                drawKeyBox("SPACE", 2, 68, 72, 20, jump);
            }

            private void drawKeyBox(String label, float kx, float ky, float kw, float kh, boolean pressed) {
                int bg = pressed ? getAccentColor() : 0x66161B22;
                int fg = pressed ? 0xFF0B0E14 : 0xFFE6EDF3;
                GL4ESRenderUtils.drawRoundedRect(kx, ky, kw, kh, 3.0f, bg);
                drawCenteredText(label, kx + kw / 2.0f, ky + (kh - 8.0f) / 2.0f, fg);
            }
        });

        // 4. Coordinates HUD
        register(new HudModule("coordinates", "Coordinates", "الإحداثيات", "Displays player X, Y, Z position and cardinal direction", "يعرض إحداثيات اللاعب واتجاه النظر بدقة", true, 8, 158, 138, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedCoordsText, getWidth() / 2.0f, 7.0f, 0xFFE6EDF3);
            }
        });

        // 5. Direction Compass HUD
        register(new HudModule("direction", "Direction HUD", "بوصلة الاتجاه", "Displays cardinal direction and yaw angle ribbon", "يعرض بوصلة الاتجاه وزاوية النظر الأفقية", true, 152, 8, 92, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedDirectionText, getWidth() / 2.0f, 7.0f, 0xFF00A8FF);
            }
        });

        // 6. Armor & Held Item Status HUD
        register(new HudModule("armor_status", "Armor Status", "حالة الدروع", "Displays equipped armor pieces and remaining durability", "يعرض الدروع المرتداة ومتانتها المتبقية", true, 8, 184, 92, 70) {
            @Override
            protected void renderContent(boolean editMode) {
                if (mc.thePlayer == null) {
                    ArabicTextRenderer.drawString("Helm: 100%", 6, 6, 0xFF3FB950, true);
                    ArabicTextRenderer.drawString("Chest: 98%", 6, 22, 0xFF3FB950, true);
                    ArabicTextRenderer.drawString("Legs: 95%", 6, 38, 0xFF00A8FF, true);
                    ArabicTextRenderer.drawString("Boots: 92%", 6, 54, 0xFF00A8FF, true);
                    return;
                }
                boolean anyArmor = false;
                int yOffset = 3;
                GlStateManager.pushMatrix();
                try {
                    RenderHelper.enableGUIStandardItemLighting();
                    for (int i = 3; i >= 0; i--) {
                        ItemStack stack = mc.thePlayer.inventory.armorInventory[i];
                        if (stack != null) {
                            anyArmor = true;
                            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 4, yOffset);
                            int max = stack.getMaxDamage();
                            int dmg = stack.getItemDamage();
                            int rem = Math.max(0, max - dmg);
                            int pct = max > 0 ? (rem * 100) / max : 100;
                            int col = pct > 50 ? 0xFF3FB950 : (pct > 25 ? 0xFFD29922 : 0xFFF85149);
                            RenderHelper.disableStandardItemLighting();
                            GL4ESRenderUtils.resetGlState();
                            ArabicTextRenderer.drawString(String.valueOf(rem), 24, yOffset + 4, col, true);
                            RenderHelper.enableGUIStandardItemLighting();
                        }
                        yOffset += 16;
                    }
                } finally {
                    RenderHelper.disableStandardItemLighting();
                    GL4ESRenderUtils.resetGlState();
                    GlStateManager.popMatrix();
                }

                if (!anyArmor) {
                    drawCenteredText(editMode ? "Armor Preview" : "No Armor", getWidth() / 2.0f, 29.0f, 0xFF8B949E);
                }
            }
        });

        // 7. Potion Effects & Potion Counter HUD
        register(new HudModule("potion_effects", "Potion Status", "حالة وعداد الجرعات", "Displays active potion timers and splash healing potion count", "يعرض مؤقتات الجرعات النشطة وعدد جرعات العلاج في المخزون", true, 106, 184, 118, 54) {
            @Override
            protected void renderContent(boolean editMode) {
                if (mc.thePlayer == null) {
                    ArabicTextRenderer.drawString("Speed II 01:24", 6, 6, 0xFF00A8FF, true);
                    ArabicTextRenderer.drawString("Strength I 00:45", 6, 20, 0xFFF85149, true);
                    ArabicTextRenderer.drawString("Heal Pots: 12", 6, 36, 0xFF3FB950, true);
                    return;
                }
                int splashPots = countSplashPotions();
                Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
                int yOff = 5;
                int shown = 0;
                if (effects != null && !effects.isEmpty()) {
                    for (PotionEffect effect : effects) {
                        if (shown >= 2) break;
                        String name = I18n.format(effect.getEffectName());
                        int amp = effect.getAmplifier() + 1;
                        String dur = Potion.getDurationString(effect);
                        ArabicTextRenderer.drawString(name + " " + amp + " (" + dur + ")", 6, yOff, 0xFFE6EDF3, true);
                        yOff += 14;
                        shown++;
                    }
                } else {
                    ArabicTextRenderer.drawString("No Active Effects", 6, yOff, 0xFF8B949E, true);
                    yOff += 14;
                }
                ArabicTextRenderer.drawString("Splash Pots: " + splashPots, 6, 36, 0xFF3FB950, true);
            }
        });

        // 8. Target HUD (Opponent Health & Distance Inspector)
        register(new HudModule("target_hud", "Target HUD", "بطاقة الخصم Target HUD", "Displays opponent name, health bar, and distance during combat", "يعرض اسم الخصم وشريط صحته ومسافته أثناء القتال", true, 250, 34, 128, 38) {
            @Override
            protected void renderContent(boolean editMode) {
                EntityLivingBase target = resolveCurrentTarget();
                if (target == null) {
                    if (editMode) {
                        ArabicTextRenderer.drawString("Target: Opponent", 6, 5, 0xFFE6EDF3, true);
                        ArabicTextRenderer.drawString("18.5 HP | 2.7m", 6, 16, 0xFF3FB950, true);
                        GL4ESRenderUtils.drawRoundedRect(6, 28, 116, 5, 2.0f, 0xFF21262D);
                        GL4ESRenderUtils.drawRoundedRect(6, 28, 102, 5, 2.0f, 0xFF3FB950);
                    } else {
                        drawCenteredText("Target: Ready", getWidth() / 2.0f, 15.0f, 0xFF8B949E);
                    }
                    return;
                }
                String name = target.getName();
                float hp = target.getHealth();
                float maxHp = Math.max(1.0f, target.getMaxHealth());
                float ratio = MathHelper.clamp_float(hp / maxHp, 0.0f, 1.0f);
                float dist = mc.thePlayer != null ? mc.thePlayer.getDistanceToEntity(target) : 0.0f;
                int distWhole = (int) dist;
                int distFrac = Math.abs((int) ((dist - distWhole) * 10));

                ArabicTextRenderer.drawString(name, 6, 5, 0xFFE6EDF3, true);
                ArabicTextRenderer.drawString(((int) hp) + " HP | " + distWhole + "." + distFrac + "m", 6, 16, 0xFF00A8FF, true);
                GL4ESRenderUtils.drawRoundedRect(6, 28, 116, 5, 2.0f, 0xFF21262D);
                int barCol = ratio > 0.5f ? 0xFF3FB950 : (ratio > 0.25f ? 0xFFD29922 : 0xFFF85149);
                GL4ESRenderUtils.drawRoundedRect(6, 28, 116.0f * ratio, 5, 2.0f, barCol);
            }
        });

        // 9. Ping Display HUD
        register(new HudModule("ping", "Ping Display", "زمن الاستجابة Ping", "Displays real server latency from NetHandlerPlayClient", "يعرض سرعة استجابة الاتصال بالسيرفر", true, 98, 34, 74, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedPingText, getWidth() / 2.0f, 7.0f, 0xFF3FB950);
            }
        });

        // 10. Combo Counter HUD
        register(new HudModule("combo", "Combo Counter", "عداد الضربات المتتالية", "Tracks consecutive melee hits landed on opponents", "يحسب عدد الضربات المتتالية في القتال", true, 88, 60, 84, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                if (System.currentTimeMillis() - lastHitTimestamp > 3500L) {
                    currentCombo = 0;
                }
                String text = currentCombo > 0 ? (currentCombo + " Combo") : "0 Combo";
                drawCenteredText(text, getWidth() / 2.0f, 7.0f, 0xFFA371F7);
            }
        });

        // 11. Reach Display HUD
        register(new HudModule("reach", "Reach Display", "مسافة الضربة Reach", "Displays local hit distance on your last melee attack", "يعرض مسافة الهدف المحلية عند تسجيل الضربة", true, 88, 86, 84, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                if (System.currentTimeMillis() - lastHitTimestamp > 4000L) {
                    lastReachDistance = 0.0f;
                }
                int whole = (int) lastReachDistance;
                int frac = Math.abs((int) ((lastReachDistance - whole) * 100));
                String text = whole + "." + (frac < 10 ? "0" + frac : frac) + " blocks";
                drawCenteredText(text, getWidth() / 2.0f, 7.0f, 0xFFE6EDF3);
            }
        });

        // 12. Memory & Performance Monitor HUD
        register(new HudModule("memory", "Memory & Perf", "مراقب الذاكرة والأداء", "Displays JVM heap allocation MB and percentage safely", "يعرض استهلاك ذاكرة JVM ونسبتها المئوية بدقة", true, 250, 8, 112, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedMemText, getWidth() / 2.0f, 7.0f, 0xFFE6EDF3);
            }
        });

        // 13. Clock HUD
        register(new HudModule("clock", "Clock Display", "الساعة الوقتية", "Displays real-time local clock without frame lag", "يعرض الوقت المحلي الحالي بتحديث خفيف", false, 88, 112, 84, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedClockText, getWidth() / 2.0f, 7.0f, 0xFFE6EDF3);
            }
        });

        // 14. Server Address HUD
        register(new HudModule("server_address", "Server IP HUD", "عنوان السيرفر", "Displays current server IP or hides it when Nick Hider is active", "يعرض عنوان السيرفر الحالي مع احترام ميزة إخفاء الهوية", false, 88, 136, 108, 20) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedServerText, getWidth() / 2.0f, 6.0f, 0xFF8B949E);
            }
        });

        // 15. PvP Session Stats & Stopwatch HUD
        register(new HudModule("session_stats", "PvP Session Stats", "إحصائيات الجلسة القتالية", "Tracks session duration, total hits, max combo, and peak CPS", "يتتبع مدة الجلسة وإجمالي الضربات وأعلى كومبو وأعلى CPS", false, 230, 184, 124, 54) {
            @Override
            protected void renderContent(boolean editMode) {
                long elapsedSec = Math.max(0L, (System.currentTimeMillis() - sessionStartMs) / 1000L);
                long mins = elapsedSec / 60L;
                long secs = elapsedSec % 60L;
                ArabicTextRenderer.drawString("Time: " + mins + "m " + secs + "s", 6, 6, 0xFFE6EDF3, true);
                ArabicTextRenderer.drawString("Hits: " + totalSessionHits + " | Max: " + maxSessionCombo, 6, 20, 0xFF00A8FF, true);
                ArabicTextRenderer.drawString("Peak CPS: " + peakSessionCps, 6, 34, 0xFF3FB950, true);
            }
        });

        // 16. Cooldowns & TNT Timer HUD
        register(new HudModule("cooldowns", "Cooldowns & TNT", "مؤقتات اللؤلؤ والـ TNT", "Displays Ender Pearl cooldown and nearby primed TNT fuse timer", "يعرض مؤقت لؤلؤة الإندر والعد التنازلي للـ TNT القريب", true, 178, 34, 68, 36) {
            @Override
            protected void renderContent(boolean editMode) {
                long now = System.currentTimeMillis();
                float pearlRem = Math.max(0.0f, 15.0f - (now - lastEnderPearlUseMs) / 1000.0f);
                float tntSec = findNearestPrimedTntSeconds();
                String line1 = pearlRem > 0.1f ? ("Pearl: " + ((int) pearlRem) + "s") : "Pearl: OK";
                String line2 = tntSec > 0.0f ? ("TNT: " + ((int) (tntSec * 10) / 10.0f) + "s") : "TNT: Safe";
                ArabicTextRenderer.drawString(line1, 6, 6, pearlRem > 0.1f ? 0xFFD29922 : 0xFF3FB950, true);
                ArabicTextRenderer.drawString(line2, 6, 20, tntSec > 0.0f ? 0xFFF85149 : 0xFF8B949E, true);
            }
        });

        // 17. Saturation & Stamina HUD
        register(new HudModule("saturation", "Saturation HUD", "مؤشر الإشباع الغذائي", "Displays exact food saturation level alongside hunger", "يعرض مستوى الإشباع الغذائي الدقيق للاعب", false, 178, 74, 68, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                float sat = 20.0f;
                if (mc.thePlayer != null && mc.thePlayer.getFoodStats() != null) {
                    sat = mc.thePlayer.getFoodStats().getSaturationLevel();
                }
                int whole = (int) sat;
                int frac = Math.abs((int) ((sat - whole) * 10));
                drawCenteredText("Sat: " + whole + "." + frac, getWidth() / 2.0f, 7.0f, 0xFFD29922);
            }
        });

        // 18. Pack Display HUD
        register(new HudModule("pack_display", "Pack Display", "عرض ريسورس باك", "Displays the currently active resource pack on HUD", "يعرض اسم حزمة الموارد النشطة حالياً على الشاشة", false, 178, 100, 104, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedPackText, getWidth() / 2.0f, 7.0f, 0xFFA371F7);
            }
        });

        // 19. BedWars Resource Tracker HUD
        register(new HudModule("resource_tracker", "Resource Tracker", "متتبع موارد BedWars", "Counts Iron, Gold, Diamonds, and Emeralds in inventory", "يعرض عدد الحديد والذهب والألماس والزمرد في مخزونك", true, 178, 126, 148, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedResourceText, getWidth() / 2.0f, 7.0f, 0xFF3FB950);
            }
        });

        // 20. Sprint & W-Tap Rhythm HUD
        register(new HudModule("sprint_status", "Sprint & W-Tap HUD", "حالة الركض وتوقيت W-Tap", "Shows live Sprint/Sneak toggle state and W-Tap reset window", "يعرض حالة تثبيت الركض ونافذة توقيت W-Tap بعد الضربة", true, 178, 152, 132, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                long sinceHit = System.currentTimeMillis() - lastHitTimestamp;
                String status;
                int col;
                if (sinceHit >= 60L && sinceHit <= 260L) {
                    status = "[W-Tap Window Ready]";
                    col = 0xFF3FB950;
                } else if (mc.thePlayer != null && mc.thePlayer.isSneaking()) {
                    status = sneakToggled ? "[Sneaking (Toggled)]" : "[Sneaking (Held)]";
                    col = 0xFFD29922;
                } else if (isModuleEnabled("toggle_sprint") && sprintToggled) {
                    status = "[Sprinting (Toggled)]";
                    col = 0xFF00A8FF;
                } else {
                    status = "[Walking]";
                    col = 0xFF8B949E;
                }
                drawCenteredText(status, getWidth() / 2.0f, 7.0f, col);
            }
        });

        // ====================================================================
        // 2. PVP, VISUALS, INVENTORY & PERFORMANCE MODULES (22 Modules)
        // ====================================================================

        // 21. 1.8.9 Hit Delay Fix (MixinMinecraft leftClickCounter = 0)
        register(new Module("hit_delay_fix", "1.8.9 Hit Delay Fix", "إصلاح تأخير الضرب 1.8.9", "Removes the vanilla 1.8.9 10-tick attack block after a missed swing via MixinMinecraft", "يزيل عقوبة تأخير الضرب (10 تيك) في 1.8.9 بعد التلويح بالخطأ لضمان تسجيل كل ضربة فوراً", ModuleCategory.PVP, true, 0) {
        });

        // 22. Toggle Sprint
        register(new Module("toggle_sprint", "Toggle Sprint", "تثبيت الركض التلقائي", "Maintains sprint state automatically when moving forward", "يثبت وضع الركض تلقائياً عند التقدم للأمام", ModuleCategory.PVP, true, Keyboard.KEY_I) {
            @Override
            protected void onEnable() {
                super.onEnable();
                sprintToggled = true;
            }

            @Override
            protected void onDisable() {
                super.onDisable();
                sprintToggled = false;
            }
        });

        // 23. Toggle Sneak
        register(new Module("toggle_sneak", "Toggle Sneak", "تثبيت التسلل Sneak", "Toggles sneak state safely while in-game (disengaged in GUIs)", "يثبت وضع الانحناء داخل اللعب ويتوقف تلقائياً عند فتح القوائم", ModuleCategory.PVP, false, Keyboard.KEY_V) {
            @Override
            protected void onEnable() {
                super.onEnable();
                sneakToggled = true;
            }

            @Override
            protected void onDisable() {
                super.onDisable();
                sneakToggled = false;
                if (mc.gameSettings != null) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                }
            }
        });

        // 24. Freelook 360 (CameraSetup Hook)
        register(new Module("freelook", "Freelook 360", "منظور الكاميرا الحر", "Hold Left Alt (LMENU) to rotate camera 360 degrees without changing movement direction", "اضغط باستمرار على Left Alt لتحريك الكاميرا 360 درجة دون تغيير اتجاه الحركة", ModuleCategory.PVP, true, Keyboard.KEY_LMENU) {
            @Override
            protected void onDisable() {
                super.onDisable();
                if (freelookActive && mc.gameSettings != null) {
                    freelookActive = false;
                    mc.gameSettings.thirdPersonView = savedThirdPersonView;
                }
            }
        });

        // 25. Smooth Zoom (FOVModifier Hook)
        register(new Module("zoom", "Smooth Zoom", "التقريب البصري السلس Zoom", "Hold C key in-game for 4x optical zoom with smooth camera", "اضغط باستمرار على حرف C للتقريب البصري 4x مع تنعيم حركة الكاميرا", ModuleCategory.PVP, true, Keyboard.KEY_C) {
        });

        // 26. Snaplook Rear View
        register(new Module("snaplook", "Snaplook Rear View", "نظرة خلفية سريعة Snaplook", "Hold F key for instant rear third-person camera check", "اضغط باستمرار على حرف F لإلقاء نظرة خلفية فورية", ModuleCategory.PVP, true, Keyboard.KEY_F) {
            @Override
            protected void onDisable() {
                super.onDisable();
                if (snaplookActive && mc.gameSettings != null) {
                    snaplookActive = false;
                    mc.gameSettings.thirdPersonView = savedSnaplookView;
                }
            }
        });

        // 27. Dynamic FOV Controller
        register(new Module("fov_mod", "Static PvP FOV", "تثبيت زاوية الرؤية FOV", "Prevents Speed potions and sprinting from warping your aim FOV", "يمنع تغير زاوية الرؤية أثناء الركض أو تأثيرات السرعة لثبات التصويب", ModuleCategory.PVP, false, 0) {
        });

        // 28. Custom PvP Crosshair
        register(new Module("crosshair", "Custom Crosshair", "مؤشر التصويب الاحترافي", "Replaces vanilla crosshair with a crisp GL4ES-safe dot & gap reticle that turns red on targets", "يستبدل علامة التصويب بمؤشر قتالي دقيق يتغير لونه عند التصويب على الخصم", ModuleCategory.PVP, true, 0) {
        });

        // 29. Crosshair Hit Markers
        register(new Module("hit_markers", "Hit Markers", "علامة الإصابة Hit Markers", "Flashes diagonal hit-confirmation ticks around crosshair for 250ms on melee hit", "يرسم علامة إصابة قطرية حول مؤشر التصويب فور تسجيل الضربة على الخصم", ModuleCategory.VISUALS, true, 0) {
        });

        // 30. HurtCam Control (MixinEntityRenderer)
        register(new Module("hurtcam", "HurtCam Control", "تقليل اهتزاز الضرر", "Reduces screen tilt intensity (35% default) when taking damage", "يقلل اهتزاز الشاشة عند تلقي الضرر لتحسين ثبات التصويب", ModuleCategory.VISUALS, true, 0) {
        });

        // 31. Hit Color Customizer (MixinRendererLivingEntity)
        register(new Module("hitcolor", "Hit Color", "تخصيص لون الإصابة", "Customizes entity damage overlay tint color", "يغير لون الخصم بصرياً عند تسجيل الإصابة", ModuleCategory.VISUALS, true, 0) {
        });

        // 32. Fullbright Gamma Override
        register(new Module("fullbright", "Fullbright", "إضاءة كاملة Fullbright", "Boosts gamma locally to 16.0 for full visibility in dark maps and caves", "يرفع مستوى الإضاءة محلياً لرؤية واضحة تماماً في الأماكن المظلمة", ModuleCategory.VISUALS, true, Keyboard.KEY_G) {
            @Override
            protected void onEnable() {
                super.onEnable();
                if (mc.gameSettings != null) {
                    if (mc.gameSettings.gammaSetting < 10.0f) {
                        savedGamma = mc.gameSettings.gammaSetting;
                    }
                    mc.gameSettings.gammaSetting = 16.0f;
                }
            }

            @Override
            protected void onDisable() {
                super.onDisable();
                if (mc.gameSettings != null) {
                    mc.gameSettings.gammaSetting = Math.min(1.5f, Math.max(0.5f, savedGamma));
                }
            }
        });

        // 33. Custom Block Outline (DrawBlockHighlightEvent)
        register(new Module("block_outline", "Block Outline", "تحديد المكعبات المضيء", "Highlights targeted blocks with an Electric Blue bounding box", "يرسم إطاراً أزرق واضحاً حول المكعب الذي تصوب نحوه", ModuleCategory.VISUALS, true, 0) {
        });

        // 34. Weather Changer (Clear Rain/Thunder)
        register(new Module("weather_changer", "Clear Weather", "طقس صافي (إزالة المطر)", "Clears client-side rain and thunder particles to prevent mobile GPU drops", "يزيل المطر والبرق محلياً لمنع هبوط الإطارات على الهواتف", ModuleCategory.VISUALS, true, 0) {
        });

        // 35. Time Changer (Daylight Override)
        register(new Module("time_changer", "Daylight Time", "تثبيت وقت النهار", "Forces clean daytime sky lighting locally during matches", "يثبت إضاءة النهار الصافية محلياً لوضوح الرؤية", ModuleCategory.VISUALS, false, 0) {
        });

        // 36. Low Health Warning Alert
        register(new Module("low_health_warning", "Low Health Alert", "تنبيه انخفاض الصحة", "Renders a pulsing crimson edge alert when health drops below 3.5 hearts", "يظهر إطاراً تحذيرياً أحمر عند انخفاض صحتك لأقل من 3.5 قلوب", ModuleCategory.VISUALS, true, 0) {
        });

        // 37. Nick Hider
        register(new Module("nick_hider", "Nick Hider", "إخفاء الاسم وعنوان السيرفر", "Masks server IP and personal nickname on HUD for safe recording", "يخفي عنوان السيرفر والاسم الشخصي أثناء التصوير أو البث", ModuleCategory.VISUALS, false, 0) {
        });

        // 38. Inventory PvP Summary Helper
        register(new Module("inventory_helper", "Inventory Helper", "مساعد المخزون القتالي", "Displays total splash potions, golden apples, arrows, and blocks at the top of inventory screens", "يعرض ملخصاً فورياً لعدد الجرعات والتفاح الذهبي والسهام والبلوكات أعلى شاشة المخزون", ModuleCategory.INVENTORY, true, 0) {
        });

        // 39. Auto GG
        register(new Module("auto_gg", "Auto GG", "رسالة GG التلقائية", "Automatically sends gg in chat when a match ends (with 5s anti-spam cooldown)", "يرسل كلمة gg تلقائياً عند انتهاء المباراة مع حماية من التكرار", ModuleCategory.INVENTORY, false, 0) {
        });

        // 40. In-Game Toast Notifications & UI Audio
        register(new Module("toast_notifications", "Toast Alerts & UI Sound", "إشعارات الواجهة والأصوات", "Shows animated corner toast notifications and plays click audio when toggling modules", "يعرض إشعارات منبثقة أنيقة مع صوت نقر خفيف عند تفعيل أو تعطيل الميزات", ModuleCategory.VISUALS, true, 0) {
        });

        // 41. Memory Watchdog & Corpse Clear
        register(new Module("memory_watchdog", "Memory Watchdog & Corpse Clear", "حارس الذاكرة وتنظيف الجثث", "Cleans lingering dead entities (deathTime >= 19) to prevent entity lag spikes", "يزيل الكيانات الميتة العالقة محلياً لمنع تقطيع الإطارات واستهلاك الذاكرة", ModuleCategory.PERFORMANCE, true, 0) {
        });

        // 42. GL4ES Frame Pacer & Particle Optimizer
        register(new Module("gl4es_optimizer", "GL4ES Frame Pacer", "محسن أداء GL4ES للهواتف", "Throttles HUD string allocations to 5Hz and reduces particle load when FPS drops below 45", "يقلل تخصيص النصوص إلى 5 مرات بالثانية ويخفف الجزيئات تلقائياً عند انخفاض الإطارات على Adreno 710", ModuleCategory.PERFORMANCE, true, 0) {
            @Override
            protected void onEnable() {
                super.onEnable();
                if (mc.gameSettings != null) {
                    savedParticleSetting = mc.gameSettings.particleSetting;
                }
            }

            @Override
            protected void onDisable() {
                super.onDisable();
                if (mc.gameSettings != null) {
                    mc.gameSettings.particleSetting = savedParticleSetting;
                }
            }
        });
    }

    private void register(Module module) {
        modules.put(module.getId(), module);
        if (module instanceof HudModule) {
            hudModules.add((HudModule) module);
        }
    }

    // ========================================================================
    // UI AUDIO & TOAST NOTIFICATION ENGINE
    // ========================================================================

    public void playUiClickSound() {
        if (!isModuleEnabled("toast_notifications")) {
            return;
        }
        try {
            if (mc.getSoundHandler() != null) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(BUTTON_CLICK_SOUND, 1.0F));
            }
        } catch (Throwable ignored) {
        }
    }

    public void pushToast(String title, String subtitle, int accentColor) {
        if (!isModuleEnabled("toast_notifications")) {
            return;
        }
        long now = System.currentTimeMillis();
        this.toastTitle = title != null ? title : "NEXORA";
        this.toastSubtitle = subtitle != null ? subtitle : "";
        this.toastAccentColor = accentColor;
        this.toastStartMs = now;
        this.toastExpireMs = now + 2200L;
    }

    public void pushModuleToggleToast(Module m) {
        if (m == null) {
            return;
        }
        boolean rtl = I18nManager.isRtl();
        String name = rtl ? m.getNameAr() : m.getNameEn();
        String state = m.isEnabled() ? (rtl ? "تم التفعيل (ON)" : "Enabled (ON)") : (rtl ? "تم التعطيل (OFF)" : "Disabled (OFF)");
        int col = m.isEnabled() ? 0xFF3FB950 : 0xFFF85149;
        pushToast(name, state, col);
    }

    // ========================================================================
    // 3-LAYER HUD RENDER PIPELINE (Guaranteed Visibility in Forge 1.8.9)
    // ========================================================================

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            currentRenderFrameId++;
            long nowNano = System.nanoTime();
            float deltaMs = (nowNano - lastFrameNano) / 1_000_000.0f;
            lastFrameNano = nowNano;
            if (deltaMs > 0.1f && deltaMs < 1000.0f) {
                currentFrameTimeMs = deltaMs;
                frameTimesMs[frameTimeHead] = deltaMs;
                frameTimeHead = (frameTimeHead + 1) % frameTimesMs.length;
                if (frameTimeCount < frameTimesMs.length) {
                    frameTimeCount++;
                }
            }
            if (isModuleEnabled("time_changer") && mc.theWorld != null) {
                mc.theWorld.setWorldTime(6000L);
            }
        } else if (event.phase == TickEvent.Phase.END) {
            // Layer 3 safety net: if in-world and neither RenderGameOverlayEvent.Post nor MixinGuiIngame fired this frame
            if (lastHudRenderedFrameId != currentRenderFrameId
                && mc.theWorld != null
                && mc.thePlayer != null
                && !mc.gameSettings.hideGUI
                && !mc.gameSettings.showDebugInfo
                && !(mc.currentScreen instanceof GuiHudEditor)) {
                lastHudRenderedFrameId = currentRenderFrameId;
                ScaledResolution sr = new ScaledResolution(mc);
                renderAllHud(sr, false);
            }
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event) {
        // Layer 1 primary Forge 1.8.9 hook (GuiIngameForge posts ElementType.ALL and ElementType.TEXT)
        if (event.type != RenderGameOverlayEvent.ElementType.ALL
            && event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        if (mc.gameSettings.showDebugInfo || mc.currentScreen instanceof GuiHudEditor) {
            return;
        }
        if (lastHudRenderedFrameId == currentRenderFrameId) {
            return;
        }
        lastHudRenderedFrameId = currentRenderFrameId;
        ScaledResolution sr = event.resolution != null ? event.resolution : new ScaledResolution(mc);
        renderAllHud(sr, false);
    }

    /**
     * Layer 2 fallback called by MixinGuiIngame if vanilla GuiIngame.renderGameOverlay is active.
     */
    public void renderAllHudFromMixin(ScaledResolution sr) {
        if (mc.gameSettings.showDebugInfo || mc.currentScreen instanceof GuiHudEditor) {
            return;
        }
        if (lastHudRenderedFrameId == currentRenderFrameId) {
            return;
        }
        lastHudRenderedFrameId = currentRenderFrameId;
        renderAllHud(sr, false);
    }

    public void renderAllHud(ScaledResolution sr, boolean editMode) {
        if (sr == null) {
            sr = new ScaledResolution(mc);
        }
        GlStateManager.pushMatrix();
        try {
            // 1. Low Health Warning Edge Alert (if player HP <= 7.0)
            if (!editMode && isModuleEnabled("low_health_warning") && mc.thePlayer != null && mc.thePlayer.getHealth() <= 7.0f && mc.thePlayer.getHealth() > 0.0f) {
                renderLowHealthOverlay(sr);
            }

            // 2. Render all Draggable HUD Widgets
            for (int i = 0, size = hudModules.size(); i < size; i++) {
                HudModule hud = hudModules.get(i);
                hud.renderHud(sr, editMode);
            }

            // 3. Render Animated In-Game Toast Notification
            if (!editMode && isModuleEnabled("toast_notifications")) {
                renderActiveToast(sr);
            }
        } finally {
            GL4ESRenderUtils.resetGlState();
            if (!editMode && mc.getTextureManager() != null) {
                mc.getTextureManager().bindTexture(Gui.icons);
            }
            GlStateManager.popMatrix();
        }
    }

    private void renderLowHealthOverlay(ScaledResolution sr) {
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        float pulse = (float) (0.35 + 0.25 * Math.sin(System.currentTimeMillis() / 180.0));
        int alpha = Math.min(140, Math.max(30, (int) (pulse * 160.0f)));
        int col = (alpha << 24) | 0x00F85149;
        GL4ESRenderUtils.drawRect(0, 0, sw, 4, col);
        GL4ESRenderUtils.drawRect(0, sh - 4, sw, sh, col);
        GL4ESRenderUtils.drawRect(0, 4, 4, sh - 4, col);
        GL4ESRenderUtils.drawRect(sw - 4, 4, sw, sh - 4, col);
    }

    private void renderActiveToast(ScaledResolution sr) {
        long now = System.currentTimeMillis();
        if (now >= toastExpireMs || toastTitle.isEmpty()) {
            return;
        }
        float w = 136.0f;
        float h = 30.0f;
        float elapsed = (float) (now - toastStartMs);
        float rem = (float) (toastExpireMs - now);
        float slideFactor = 1.0f;
        if (elapsed < 180.0f) {
            slideFactor = elapsed / 180.0f;
        } else if (rem < 220.0f) {
            slideFactor = rem / 220.0f;
        }
        float tx = sr.getScaledWidth() - (w + 8.0f) * slideFactor;
        float ty = sr.getScaledHeight() - h - 10.0f;

        GL4ESRenderUtils.drawBorderedRoundedRect(tx, ty, w, h, 5.0f, 1.2f, 0xEC111621, toastAccentColor);
        GL4ESRenderUtils.drawRoundedRect(tx + 4.0f, ty + 5.0f, 3.0f, h - 10.0f, 1.5f, toastAccentColor);
        ArabicTextRenderer.drawString(toastTitle, tx + 11.0f, ty + 5.0f, 0xFFE6EDF3, true);
        ArabicTextRenderer.drawString(toastSubtitle, tx + 11.0f, ty + 17.0f, toastAccentColor, false);
    }

    // ========================================================================
    // CUSTOM CROSSHAIR, HIT MARKERS, BLOCK OUTLINE, FREELOOK, SNAPLOOK & ZOOM
    // ========================================================================

    @SubscribeEvent
    public void onRenderCrosshairPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        boolean customCrosshair = isModuleEnabled("crosshair") && mc.gameSettings.thirdPersonView == 0;
        boolean showHitMarker = isModuleEnabled("hit_markers") && (System.currentTimeMillis() - lastHitTimestamp <= 250L);

        if (!customCrosshair && !showHitMarker) {
            return;
        }

        ScaledResolution sr = event.resolution != null ? event.resolution : new ScaledResolution(mc);
        float cx = sr.getScaledWidth() / 2.0f;
        float cy = sr.getScaledHeight() / 2.0f;

        if (customCrosshair) {
            event.setCanceled(true);
            boolean aimingAtEntity = mc.objectMouseOver != null
                && mc.objectMouseOver.entityHit instanceof EntityLivingBase;
            int color = aimingAtEntity ? 0xFFFF3B30 : clientThemeColor;
            float gap = aimingAtEntity ? 2.5f : 3.5f;
            float len = 4.0f;
            float thick = 1.0f;

            // Center dot
            GL4ESRenderUtils.drawRect(cx - 0.75f, cy - 0.75f, cx + 0.75f, cy + 0.75f, 0xFFFFFFFF);
            // Top, Bottom, Left, Right arms
            GL4ESRenderUtils.drawRect(cx - thick / 2.0f, cy - gap - len, cx + thick / 2.0f, cy - gap, color);
            GL4ESRenderUtils.drawRect(cx - thick / 2.0f, cy + gap, cx + thick / 2.0f, cy + gap + len, color);
            GL4ESRenderUtils.drawRect(cx - gap - len, cy - thick / 2.0f, cx - gap, cy + thick / 2.0f, color);
            GL4ESRenderUtils.drawRect(cx + gap, cy - thick / 2.0f, cx + gap + len, cy + thick / 2.0f, color);
        }

        if (showHitMarker) {
            int hmColor = 0xFFFF3B30;
            GL4ESRenderUtils.drawLine(cx - 6.0f, cy - 6.0f, cx - 2.5f, cy - 2.5f, 1.5f, hmColor);
            GL4ESRenderUtils.drawLine(cx + 2.5f, cy - 2.5f, cx + 6.0f, cy - 6.0f, 1.5f, hmColor);
            GL4ESRenderUtils.drawLine(cx - 6.0f, cy + 6.0f, cx - 2.5f, cy + 2.5f, 1.5f, hmColor);
            GL4ESRenderUtils.drawLine(cx + 2.5f, cy + 2.5f, cx + 6.0f, cy + 6.0f, 1.5f, hmColor);
        }

        if (mc.getTextureManager() != null) {
            mc.getTextureManager().bindTexture(Gui.icons);
        }
        GL4ESRenderUtils.resetGlState();
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (!isModuleEnabled("block_outline") || event.target == null) {
            return;
        }
        if (event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mc.theWorld == null) {
            return;
        }
        BlockPos pos = event.target.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block.getMaterial() == Material.air || !mc.theWorld.getWorldBorder().contains(pos)) {
            return;
        }
        event.setCanceled(true);

        Entity player = event.player;
        double interpX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double interpY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double interpZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        block.setBlockBoundsBasedOnState(mc.theWorld, pos);
        AxisAlignedBB box = block.getSelectedBoundingBox(mc.theWorld, pos)
            .expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D)
            .offset(-interpX, -interpY, -interpZ);

        float r = (float) (clientThemeColor >> 16 & 255) / 255.0F;
        float g = (float) (clientThemeColor >> 8 & 255) / 255.0F;
        float b = (float) (clientThemeColor & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(r, g, b, 0.85F);
        GL11.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        RenderGlobal.drawSelectionBoundingBox(box);

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GL11.glLineWidth(1.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (mc.thePlayer == null || mc.gameSettings == null) {
            return;
        }

        // 1. Snaplook Rear View Hold Check
        Module snapMod = modules.get("snaplook");
        if (snapMod != null && snapMod.isEnabled()) {
            int snapKey = snapMod.getKeyCode() > 0 ? snapMod.getKeyCode() : Keyboard.KEY_F;
            boolean snapHeld = mc.currentScreen == null && Keyboard.isKeyDown(snapKey);
            if (snapHeld) {
                if (!snaplookActive) {
                    snaplookActive = true;
                    savedSnaplookView = mc.gameSettings.thirdPersonView;
                    mc.gameSettings.thirdPersonView = 2;
                }
                return;
            } else if (snaplookActive) {
                snaplookActive = false;
                mc.gameSettings.thirdPersonView = savedSnaplookView;
            }
        } else if (snaplookActive) {
            snaplookActive = false;
            mc.gameSettings.thirdPersonView = savedSnaplookView;
        }

        // 2. Freelook 360 Hold Check
        Module freelookMod = modules.get("freelook");
        if (freelookMod == null || !freelookMod.isEnabled()) {
            if (freelookActive) {
                freelookActive = false;
                mc.gameSettings.thirdPersonView = savedThirdPersonView;
            }
            return;
        }
        int key = freelookMod.getKeyCode() > 0 ? freelookMod.getKeyCode() : Keyboard.KEY_LMENU;
        boolean keyHeld = mc.currentScreen == null && Keyboard.isKeyDown(key);
        if (keyHeld) {
            if (!freelookActive) {
                freelookActive = true;
                freelookYaw = mc.thePlayer.rotationYaw;
                freelookPitch = mc.thePlayer.rotationPitch;
                savedThirdPersonView = mc.gameSettings.thirdPersonView;
                mc.gameSettings.thirdPersonView = 1;
            }
            float dx = Mouse.getDX() * 0.15f;
            float dy = Mouse.getDY() * 0.15f;
            freelookYaw += dx;
            freelookPitch = MathHelper.clamp_float(freelookPitch - dy, -90.0f, 90.0f);
            event.yaw = freelookYaw;
            event.pitch = freelookPitch;
        } else if (freelookActive) {
            freelookActive = false;
            mc.gameSettings.thirdPersonView = savedThirdPersonView;
        }
    }

    @SubscribeEvent
    public void onFovModifier(EntityViewRenderEvent.FOVModifier event) {
        if (isModuleEnabled("fov_mod") && mc.gameSettings != null) {
            event.setFOV(mc.gameSettings.fovSetting);
        }
        Module zoomMod = modules.get("zoom");
        if (zoomMod != null && zoomMod.isEnabled() && mc.currentScreen == null) {
            int key = zoomMod.getKeyCode() > 0 ? zoomMod.getKeyCode() : Keyboard.KEY_C;
            if (Keyboard.isKeyDown(key)) {
                event.setFOV(event.getFOV() * 0.25f);
                zoomActive = true;
                mc.gameSettings.smoothCamera = true;
            } else if (zoomActive) {
                zoomActive = false;
                if (mc.gameSettings != null) {
                    mc.gameSettings.smoothCamera = false;
                }
            }
        } else if (zoomActive) {
            zoomActive = false;
            if (mc.gameSettings != null) {
                mc.gameSettings.smoothCamera = false;
            }
        }
    }

    @SubscribeEvent
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!isModuleEnabled("inventory_helper") || !(event.gui instanceof GuiContainer) || mc.thePlayer == null) {
            return;
        }
        int pots = countSplashPotions();
        int gapples = countItem(Items.golden_apple);
        int arrows = countItem(Items.arrow);
        int blocks = countBlocks();

        ScaledResolution sr = new ScaledResolution(mc);
        float boxW = 240.0f;
        float boxH = 20.0f;
        float boxX = (sr.getScaledWidth() - boxW) / 2.0f;
        float boxY = 6.0f;

        GL4ESRenderUtils.drawBorderedRoundedRect(boxX, boxY, boxW, boxH, 4.0f, 1.0f, 0xD9111621, clientThemeColor);
        String summary = "Pots: " + pots + "  |  Gapples: " + gapples + "  |  Blocks: " + blocks + "  |  Arrows: " + arrows;
        int tw = ArabicTextRenderer.getStringWidth(summary);
        ArabicTextRenderer.drawString(summary, boxX + (boxW - tw) / 2.0f, boxY + 6.0f, 0xFFE6EDF3, true);
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isModuleEnabled("auto_gg") || event.message == null || mc.thePlayer == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAutoGgSentMs < 5000L) {
            return;
        }
        String unformatted = event.message.getUnformattedText();
        if (unformatted != null && (unformatted.contains("1st Killer -")
            || unformatted.contains("Winner -")
            || unformatted.contains("Winning Team -")
            || unformatted.contains("Victory!"))) {
            lastAutoGgSentMs = now;
            mc.thePlayer.sendChatMessage("gg");
        }
    }

    // ========================================================================
    // TELEMETRY, CPS RING BUFFER & TICK LOGIC
    // ========================================================================

    private void updateThrottledStrings() {
        long now = System.currentTimeMillis();
        if (now - lastTelemetryUpdateMs < 200L) {
            return;
        }
        lastTelemetryUpdateMs = now;

        int fps = Minecraft.getDebugFPS();
        float sumMs = 0.0f;
        int count = Math.max(1, frameTimeCount);
        for (int i = 0; i < count; i++) {
            sumMs += frameTimesMs[i];
        }
        avgFrameTimeMs = sumMs / count;
        int msWhole = (int) avgFrameTimeMs;
        int msFrac = Math.abs((int) ((avgFrameTimeMs - msWhole) * 10));
        cachedFpsText = "FPS: " + fps + " (" + msWhole + "." + msFrac + "ms)";

        if (mc.thePlayer != null) {
            int px = MathHelper.floor_double(mc.thePlayer.posX);
            int py = MathHelper.floor_double(mc.thePlayer.posY);
            int pz = MathHelper.floor_double(mc.thePlayer.posZ);
            float yaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
            if (yaw < 0.0f) {
                yaw += 360.0f;
            }
            int dirIdx = Math.round(yaw / 45.0f) % 8;
            String dir = CARDINAL_DIRS[dirIdx];
            cachedCoordsText = "X:" + px + " Y:" + py + " Z:" + pz + " [" + dir + "]";
            cachedDirectionText = dir + " | " + Math.round(yaw) + " deg";

            int iron = countItem(Items.iron_ingot);
            int gold = countItem(Items.gold_ingot);
            int dia = countItem(Items.diamond);
            int em = countItem(Items.emerald);
            cachedResourceText = "Fe:" + iron + " · Au:" + gold + " · Dia:" + dia + " · Em:" + em;
        }

        int ping = 0;
        if (mc.getNetHandler() != null && mc.thePlayer != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (info != null) {
                ping = info.getResponseTime();
            }
        }
        cachedPingText = "Ping: " + ping + " ms";

        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;
        long usedMb = used / (1024L * 1024L);
        int pct = max > 0 ? (int) ((used * 100L) / max) : 0;
        cachedMemText = "RAM: " + pct + "% (" + usedMb + "MB)";

        cachedClockText = clockFormat.format(new Date(now));

        if (isModuleEnabled("nick_hider")) {
            cachedServerText = "Hidden Server";
        } else if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            cachedServerText = mc.getCurrentServerData().serverIP;
        } else {
            cachedServerText = "Singleplayer";
        }

        ResourcePackRepository repo = mc.getResourcePackRepository();
        if (repo != null && repo.getRepositoryEntries() != null && !repo.getRepositoryEntries().isEmpty()) {
            String rawPack = repo.getRepositoryEntries().get(0).getResourcePackName();
            cachedPackText = rawPack.length() > 16 ? rawPack.substring(0, 16) : rawPack;
        } else {
            cachedPackText = "Default 1.8.9";
        }
    }

    private void recordLeftClick(long now) {
        if (now - lastLeftClickRecordedMs < 4L) {
            return;
        }
        lastLeftClickRecordedMs = now;
        leftClicks[leftClickHead] = now;
        leftClickHead = (leftClickHead + 1) & (leftClicks.length - 1);
        if (leftClickCount < leftClicks.length) {
            leftClickCount++;
        }
    }

    private void recordRightClick(long now) {
        if (now - lastRightClickRecordedMs < 4L) {
            return;
        }
        lastRightClickRecordedMs = now;
        rightClicks[rightClickHead] = now;
        rightClickHead = (rightClickHead + 1) & (rightClicks.length - 1);
        if (rightClickCount < rightClicks.length) {
            rightClickCount++;
        }
        if (mc.thePlayer != null && mc.thePlayer.getHeldItem() != null) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held.getItem() == Items.ender_pearl) {
                lastEnderPearlUseMs = now;
            } else if (held.getItem() == Items.golden_apple) {
                lastGappleUseMs = now;
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(MouseEvent event) {
        if (!event.buttonstate) {
            return;
        }
        long now = System.currentTimeMillis();
        if (event.button == 0) {
            recordLeftClick(now);
        } else if (event.button == 1) {
            recordRightClick(now);
        }
    }

    public int getLeftCps() {
        return countRecentClicks(leftClicks, leftClickCount, System.currentTimeMillis());
    }

    public int getRightCps() {
        return countRecentClicks(rightClicks, rightClickCount, System.currentTimeMillis());
    }

    private int countRecentClicks(long[] buffer, int count, long now) {
        int total = 0;
        for (int i = 0; i < count; i++) {
            long ts = buffer[i];
            if (ts > 0L && now - ts <= 1000L) {
                total++;
            }
        }
        return total;
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.entityPlayer == mc.thePlayer && event.target != null) {
            long now = System.currentTimeMillis();
            if (now - lastHitTimestamp <= 3500L) {
                currentCombo++;
            } else {
                currentCombo = 1;
            }
            totalSessionHits++;
            if (currentCombo > maxSessionCombo) {
                maxSessionCombo = currentCombo;
            }
            lastHitTimestamp = now;
            float dist = mc.thePlayer.getDistanceToEntity(event.target) - 0.4f;
            lastReachDistance = MathHelper.clamp_float(dist, 0.0f, 6.0f);
            if (event.target instanceof EntityLivingBase) {
                lastTargetEntity = (EntityLivingBase) event.target;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // PojavLauncher touch control rising-edge CPS detection
        if (mc.gameSettings != null && mc.currentScreen == null) {
            boolean attackDown = mc.gameSettings.keyBindAttack.isKeyDown();
            if (attackDown && !prevAttackKeyDown) {
                recordLeftClick(System.currentTimeMillis());
            }
            prevAttackKeyDown = attackDown;

            boolean useDown = mc.gameSettings.keyBindUseItem.isKeyDown();
            if (useDown && !prevUseKeyDown) {
                recordRightClick(System.currentTimeMillis());
            }
            prevUseKeyDown = useDown;
        } else {
            prevAttackKeyDown = false;
            prevUseKeyDown = false;
        }

        if (mc.thePlayer == null) {
            return;
        }

        if (mc.thePlayer.hurtTime > 0) {
            currentCombo = 0;
        }

        // Toggle Sprint enforcement
        if (isModuleEnabled("toggle_sprint") && sprintToggled) {
            if (mc.thePlayer.moveForward > 0.8f
                && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isUsingItem()
                && !mc.thePlayer.isCollidedHorizontally
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6) {
                mc.thePlayer.setSprinting(true);
            }
        }

        // Toggle Sneak enforcement (disengaged when any GUI is open)
        if (isModuleEnabled("toggle_sneak") && sneakToggled && mc.gameSettings != null) {
            KeyBinding.setKeyBindState(
                mc.gameSettings.keyBindSneak.getKeyCode(),
                mc.currentScreen == null
            );
        }

        // Fullbright enforcement
        if (isModuleEnabled("fullbright") && mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = 16.0f;
        }

        // Weather Changer enforcement
        if (isModuleEnabled("weather_changer") && mc.theWorld != null) {
            mc.theWorld.setRainStrength(0.0f);
            mc.theWorld.setThunderStrength(0.0f);
        }

        // Memory Watchdog & Corpse Clear (removes lingering dead entities with deathTime >= 19)
        if (isModuleEnabled("memory_watchdog") && mc.theWorld != null) {
            List<Entity> entities = mc.theWorld.loadedEntityList;
            for (int i = entities.size() - 1; i >= 0; i--) {
                Entity e = entities.get(i);
                if (e instanceof EntityLivingBase && e != mc.thePlayer) {
                    EntityLivingBase living = (EntityLivingBase) e;
                    if (living.deathTime >= 19 && !living.isDead) {
                        living.setDead();
                        corpseCleanedCount++;
                    }
                }
            }
        }

        // GL4ES Frame Pacer adaptive particle control
        if (isModuleEnabled("gl4es_optimizer") && mc.gameSettings != null) {
            int fps = Minecraft.getDebugFPS();
            if (fps > 0 && fps < 45 && mc.gameSettings.particleSetting == 0) {
                mc.gameSettings.particleSetting = 1; // Decreased particles when FPS dips
            }
        }
    }

    private EntityLivingBase resolveCurrentTarget() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            lastTargetEntity = (EntityLivingBase) mc.objectMouseOver.entityHit;
            return lastTargetEntity;
        }
        if (lastTargetEntity != null && !lastTargetEntity.isDead && System.currentTimeMillis() - lastHitTimestamp <= 4000L) {
            return lastTargetEntity;
        }
        return null;
    }

    private float findNearestPrimedTntSeconds() {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return 0.0f;
        }
        List<Entity> list = mc.theWorld.loadedEntityList;
        int minFuse = Integer.MAX_VALUE;
        for (int i = 0, size = list.size(); i < size; i++) {
            Entity e = list.get(i);
            if (e instanceof EntityTNTPrimed && mc.thePlayer.getDistanceToEntity(e) <= 16.0f) {
                EntityTNTPrimed tnt = (EntityTNTPrimed) e;
                if (tnt.fuse > 0 && tnt.fuse < minFuse) {
                    minFuse = tnt.fuse;
                }
            }
        }
        return minFuse < Integer.MAX_VALUE ? (minFuse / 20.0f) : 0.0f;
    }

    private int countSplashPotions() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return 0;
        }
        int count = 0;
        ItemStack[] main = mc.thePlayer.inventory.mainInventory;
        for (int i = 0; i < main.length; i++) {
            ItemStack s = main[i];
            if (s != null && s.getItem() instanceof ItemPotion && ItemPotion.isSplash(s.getMetadata())) {
                count += s.stackSize;
            }
        }
        return count;
    }

    private int countItem(net.minecraft.item.Item target) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return 0;
        }
        int count = 0;
        ItemStack[] main = mc.thePlayer.inventory.mainInventory;
        for (int i = 0; i < main.length; i++) {
            ItemStack s = main[i];
            if (s != null && s.getItem() == target) {
                count += s.stackSize;
            }
        }
        return count;
    }

    private int countBlocks() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return 0;
        }
        int count = 0;
        ItemStack[] main = mc.thePlayer.inventory.mainInventory;
        for (int i = 0; i < main.length; i++) {
            ItemStack s = main[i];
            if (s != null && s.getItem() instanceof ItemBlock) {
                count += s.stackSize;
            }
        }
        return count;
    }

    public void autoArrangeHud(ScaledResolution sr) {
        if (sr == null) {
            sr = new ScaledResolution(mc);
        }
        int maxH = sr.getScaledHeight() - 10;
        float curX = 8.0f;
        float curY = 8.0f;
        float colWidth = 0.0f;

        for (int i = 0; i < hudModules.size(); i++) {
            HudModule hm = hudModules.get(i);
            if (!hm.isEnabled() || hm.isLocked()) {
                continue;
            }
            float w = hm.getWidth() * hm.getScale();
            float h = hm.getHeight() * hm.getScale();
            if (curY + h > maxH) {
                curY = 8.0f;
                curX += colWidth + 6.0f;
                colWidth = 0.0f;
            }
            hm.setX(curX);
            hm.setY(curY);
            hm.clampToScreen(sr);
            curY += h + 5.0f;
            if (w > colWidth) {
                colWidth = w;
            }
        }
    }

    public boolean isModuleEnabled(String id) {
        Module m = modules.get(id);
        return m != null && m.isEnabled();
    }

    public boolean isHitDelayFixEnabled() {
        return isModuleEnabled("hit_delay_fix");
    }

    public Module getModule(String id) {
        return modules.get(id);
    }

    public List<Module> getAllModules() {
        return new ArrayList<Module>(modules.values());
    }

    public List<HudModule> getHudModules() {
        return Collections.unmodifiableList(hudModules);
    }

    public boolean isHurtCamEnabled() {
        return isModuleEnabled("hurtcam");
    }

    public float getHurtCamMultiplier() {
        return hurtCamMultiplier;
    }

    public void setHurtCamMultiplier(float hurtCamMultiplier) {
        this.hurtCamMultiplier = MathHelper.clamp_float(hurtCamMultiplier, 0.0f, 1.0f);
    }

    public boolean isHitColorEnabled() {
        return isModuleEnabled("hitcolor");
    }

    public int getHitOverlayColor() {
        return hitOverlayColor;
    }

    public void setHitOverlayColor(int hitOverlayColor) {
        this.hitOverlayColor = hitOverlayColor;
    }

    public int getClientThemeColor() {
        return clientThemeColor;
    }

    public void setClientThemeColor(int clientThemeColor) {
        this.clientThemeColor = clientThemeColor | 0xFF000000;
    }

    public int getCorpseCleanedCount() {
        return corpseCleanedCount;
    }

    public boolean isFreelookActive() {
        return freelookActive;
    }

    public float getFreelookYaw() {
        return freelookYaw;
    }

    public float getFreelookPitch() {
        return freelookPitch;
    }

    public float getCurrentFrameTimeMs() {
        return currentFrameTimeMs;
    }

    public float getAvgFrameTimeMs() {
        return avgFrameTimeMs;
    }
}
