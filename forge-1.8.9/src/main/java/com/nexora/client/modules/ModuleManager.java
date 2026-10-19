package com.nexora.client.modules;

import com.nexora.client.hud.HudModule;
import com.nexora.client.localization.ArabicTextRenderer;
import com.nexora.client.utils.GL4ESRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NEXORA CLIENT — Central Module & HUD Registry.
 * Implements zero-allocation render loops and throttled telemetry updates
 * for optimal frame pacing on PojavLauncher (Snapdragon 6 Gen 1 / Adreno 710).
 */
public final class ModuleManager {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<String, Module> modules = new LinkedHashMap<String, Module>();
    private final List<HudModule> hudModules = new ArrayList<HudModule>();

    // Primitive ring buffers for CPS (no ArrayList<Long> boxing in render loop)
    private final long[] leftClicks = new long[128];
    private int leftClickHead = 0;
    private int leftClickCount = 0;

    private final long[] rightClicks = new long[128];
    private int rightClickHead = 0;
    private int rightClickCount = 0;

    // Combat state
    private int currentCombo = 0;
    private long lastHitTimestamp = 0L;
    private float lastReachDistance = 0.0f;

    // Throttled strings to avoid String.format per frame
    private long lastTelemetryUpdateMs = 0L;
    private String cachedFpsText = "FPS: 0";
    private String cachedCoordsText = "X: 0  Y: 0  Z: 0";
    private String cachedPingText = "Ping: 0 ms";
    private String cachedMemText = "RAM: 0%";

    // Movement & Visual state flags read by Mixins
    private boolean sprintToggled = true;
    private boolean freelookActive = false;
    private float freelookYaw = 0.0f;
    private float freelookPitch = 0.0f;
    private float hurtCamMultiplier = 0.35f;
    private int hitOverlayColor = 0x66FF2A2A;
    private float savedGamma = 1.0f;

    public void init() {
        // 1. FPS HUD
        register(new HudModule("fps", "FPS Display", "عداد الإطارات FPS", "Shows real-time measured frames per second", "يعرض معدل الإطارات الفعلي في الثانية", true, 8, 8, 76, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedFpsText, getWidth() / 2.0f, 7.0f, 0xFFE6EDF3);
            }
        });

        // 2. CPS HUD
        register(new HudModule("cps", "CPS Counter", "عداد النقرات CPS", "Left and right clicks per second using a zero-GC ring buffer", "يعرض نقرات اليسار واليمين في الثانية دون استهلاك ذاكرة", true, 8, 34, 76, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                int lmb = getLeftCps();
                int rmb = getRightCps();
                drawCenteredText(lmb + " | " + rmb + " CPS", getWidth() / 2.0f, 7.0f, 0xFF00A8FF);
            }
        });

        // 3. Keystrokes HUD
        register(new HudModule("keystrokes", "Keystrokes", "عرض أزرار الحركة", "Visualizes WASD, Space, and mouse buttons in real time", "يعرض أزرار الحركة المضغوطة والنقرات بشكل مباشر", true, 8, 62, 76, 76) {
            @Override
            protected void renderContent(boolean editMode) {
                boolean w = mc.gameSettings.keyBindForward.isKeyDown();
                boolean a = mc.gameSettings.keyBindLeft.isKeyDown();
                boolean s = mc.gameSettings.keyBindBack.isKeyDown();
                boolean d = mc.gameSettings.keyBindRight.isKeyDown();
                boolean jump = mc.gameSettings.keyBindJump.isKeyDown();

                drawKeyBox("W", 26, 2, 24, 22, w);
                drawKeyBox("A", 2, 26, 22, 22, a);
                drawKeyBox("S", 26, 26, 24, 22, s);
                drawKeyBox("D", 52, 26, 22, 22, d);
                drawKeyBox("SPACE", 2, 50, 72, 20, jump);
            }

            private void drawKeyBox(String label, float kx, float ky, float kw, float kh, boolean pressed) {
                int bg = pressed ? 0xCC00A8FF : 0x66161B22;
                int fg = pressed ? 0xFF0B0E14 : 0xFFE6EDF3;
                GL4ESRenderUtils.drawRoundedRect(kx, ky, kw, kh, 3.0f, bg);
                drawCenteredText(label, kx + kw / 2.0f, ky + (kh - 8.0f) / 2.0f, fg);
            }
        });

        // 4. Coordinates HUD
        register(new HudModule("coordinates", "Coordinates", "الإحداثيات", "Displays player X, Y, Z position and cardinal direction", "يعرض إحداثيات اللاعب واتجاه النظر بدقة", true, 8, 144, 134, 24) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedCoordsText, getWidth() / 2.0f, 8.0f, 0xFFE6EDF3);
            }
        });

        // 5. Armor Status HUD
        register(new HudModule("armor_status", "Armor Status", "حالة الدروع", "Displays equipped armor pieces and remaining durability", "يعرض الدروع المرتداة ومتانتها المتبقية", true, 8, 174, 88, 68) {
            @Override
            protected void renderContent(boolean editMode) {
                if (mc.thePlayer == null) {
                    drawCenteredText("Armor HUD", getWidth() / 2.0f, 28.0f, 0xFF8B949E);
                    return;
                }
                RenderHelper.enableGUIStandardItemLighting();
                int yOffset = 2;
                for (int i = 3; i >= 0; i--) {
                    ItemStack stack = mc.thePlayer.inventory.armorInventory[i];
                    if (stack != null) {
                        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 4, yOffset);
                        int max = stack.getMaxDamage();
                        int dmg = stack.getItemDamage();
                        int rem = Math.max(0, max - dmg);
                        ArabicTextRenderer.drawString(String.valueOf(rem), 24, yOffset + 4, 0xFFE6EDF3, true);
                    }
                    yOffset += 16;
                }
                RenderHelper.disableStandardItemLighting();
            }
        });

        // 6. Ping HUD
        register(new HudModule("ping", "Ping Display", "زمن الاستجابة Ping", "Displays real server latency from NetHandlerPlayClient", "يعرض سرعة استجابة الاتصال بالسيرفر", true, 90, 8, 74, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedPingText, getWidth() / 2.0f, 7.0f, 0xFF3FB950);
            }
        });

        // 7. Combo Counter HUD
        register(new HudModule("combo", "Combo Counter", "عداد الضربات المتتالية", "Tracks consecutive melee hits landed on opponents", "يحسب عدد الضربات المتتالية في القتال", true, 90, 34, 84, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                if (System.currentTimeMillis() - lastHitTimestamp > 3500L) {
                    currentCombo = 0;
                }
                String text = currentCombo > 0 ? (currentCombo + " Combo") : "No Combo";
                drawCenteredText(text, getWidth() / 2.0f, 7.0f, 0xFFA371F7);
            }
        });

        // 8. Reach Display HUD
        register(new HudModule("reach", "Reach Display", "مسافة الضربة Reach", "Displays local hit distance on your last melee attack", "يعرض مسافة الهدف المحلية عند تسجيل الضربة", true, 90, 60, 84, 22) {
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

        // 9. Memory Usage HUD
        register(new HudModule("memory", "Memory Usage", "استهلاك الذاكرة RAM", "Displays JVM heap allocation percentage safely", "يعرض نسبة استهلاك الذاكرة العشوائية", true, 170, 8, 82, 22) {
            @Override
            protected void renderContent(boolean editMode) {
                updateThrottledStrings();
                drawCenteredText(cachedMemText, getWidth() / 2.0f, 7.0f, 0xFFE6EDF3);
            }
        });

        // 10. Toggle Sprint / Sneak (PvP Movement)
        register(new Module("toggle_sprint", "Toggle Sprint", "تثبيت الركض التلقائي", "Maintains sprint state without holding the sprint key", "يثبت وضع الركض تلقائياً عند التقدم للأمام", ModuleCategory.PVP, true, Keyboard.KEY_I) {
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

        // 11. Freelook / Perspective 360
        register(new Module("freelook", "Freelook 360", "منظور الكاميرا الحر", "Rotate camera 360 degrees without altering movement vector", "تحريك زاوية الكاميرا بحرية دون تغيير اتجاه حركة اللاعب", ModuleCategory.PVP, true, Keyboard.KEY_LMENU) {
        });

        // 12. HurtCam Tuner
        register(new Module("hurtcam", "HurtCam Control", "تقليل اهتزاز الضرر", "Reduces screen tilt intensity when taking damage", "يقلل اهتزاز الشاشة عند تلقي الضرر لتحسين التصويب", ModuleCategory.VISUALS, true, 0) {
        });

        // 13. HitColor Customizer
        register(new Module("hitcolor", "Hit Color", "تخصيص لون الإصابة", "Customizes entity armor tint color when damaged", "يغير لون الخصم بصرياً عند تسجيل الإصابة", ModuleCategory.VISUALS, true, 0) {
        });

        // 14. Fullbright
        register(new Module("fullbright", "Fullbright", "إضاءة كاملة Fullbright", "Boosts gamma locally for clear visibility in dark caves and maps", "يرفع مستوى الإضاءة محلياً لرؤية واضحة في الأماكن المظلمة", ModuleCategory.VISUALS, true, Keyboard.KEY_G) {
            @Override
            protected void onEnable() {
                super.onEnable();
                if (mc.gameSettings != null) {
                    savedGamma = mc.gameSettings.gammaSetting;
                    mc.gameSettings.gammaSetting = 16.0f;
                }
            }

            @Override
            protected void onDisable() {
                super.onDisable();
                if (mc.gameSettings != null) {
                    mc.gameSettings.gammaSetting = savedGamma;
                }
            }
        });

        // 15. Low-Latency HUD & GL4ES Optimizer
        register(new Module("gl4es_optimizer", "GL4ES Frame Pacer", "محسن أداء GL4ES للهواتف", "Throttles non-critical HUD string formatting and minimizes GL state switches", "يقلل عمليات تبديل حالة الرسوميات وتخصيص النصوص لتحسين ثبات الإطارات على PojavLauncher", ModuleCategory.PERFORMANCE, true, 0) {
        });
    }

    private void register(Module module) {
        modules.put(module.getId(), module);
        if (module instanceof HudModule) {
            hudModules.add((HudModule) module);
        }
    }

    private void updateThrottledStrings() {
        long now = System.currentTimeMillis();
        if (now - lastTelemetryUpdateMs < 200L) {
            return;
        }
        lastTelemetryUpdateMs = now;

        int fps = Minecraft.getDebugFPS();
        cachedFpsText = "FPS: " + fps;

        if (mc.thePlayer != null) {
            BlockPos pos = mc.thePlayer.getPosition();
            cachedCoordsText = "X:" + pos.getX() + " Y:" + pos.getY() + " Z:" + pos.getZ();
        } else {
            cachedCoordsText = "X: 0  Y: 64  Z: 0";
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
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long max = rt.maxMemory();
        long used = total - free;
        int pct = max > 0 ? (int) ((used * 100L) / max) : 0;
        cachedMemText = "RAM: " + pct + "%";
    }

    @SubscribeEvent
    public void onMouseInput(MouseEvent event) {
        if (!event.buttonstate) {
            return;
        }
        long now = System.currentTimeMillis();
        if (event.button == 0) {
            leftClicks[leftClickHead] = now;
            leftClickHead = (leftClickHead + 1) & (leftClicks.length - 1);
            if (leftClickCount < leftClicks.length) leftClickCount++;
        } else if (event.button == 1) {
            rightClicks[rightClickHead] = now;
            rightClickHead = (rightClickHead + 1) & (rightClicks.length - 1);
            if (rightClickCount < rightClicks.length) rightClickCount++;
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
            if (now - buffer[i] <= 1000L) {
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
            lastHitTimestamp = now;
            float dist = mc.thePlayer.getDistanceToEntity(event.target) - 0.4f;
            lastReachDistance = MathHelper.clamp_float(dist, 0.0f, 6.0f);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) {
            return;
        }
        Module toggleSprint = modules.get("toggle_sprint");
        if (toggleSprint != null && toggleSprint.isEnabled() && sprintToggled) {
            if (mc.thePlayer.moveForward > 0.8f
                && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isUsingItem()
                && !mc.thePlayer.isCollidedHorizontally
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6) {
                mc.thePlayer.setSprinting(true);
            }
        }
    }

    public void renderAllHud(ScaledResolution sr, boolean editMode) {
        for (int i = 0, size = hudModules.size(); i < size; i++) {
            HudModule hud = hudModules.get(i);
            hud.renderHud(sr, editMode);
        }
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
        Module m = modules.get("hurtcam");
        return m != null && m.isEnabled();
    }

    public float getHurtCamMultiplier() {
        return hurtCamMultiplier;
    }

    public boolean isHitColorEnabled() {
        Module m = modules.get("hitcolor");
        return m != null && m.isEnabled();
    }

    public int getHitOverlayColor() {
        return hitOverlayColor;
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
}
