package com.nexora.client.core;

import com.nexora.client.config.ConfigManager;
import com.nexora.client.gui.GuiHudEditor;
import com.nexora.client.gui.GuiNexoraMain;
import com.nexora.client.modules.Module;
import com.nexora.client.modules.ModuleManager;
import com.nexora.client.profiles.ProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.List;

/**
 * NEXORA CLIENT — Master Forge 1.8.9 Entry Point.
 * Target: Minecraft Java Edition 1.8.9 | Forge 11.15.1.2318 | Java 8 | PojavLauncher (Android).
 */
@Mod(
    modid = NexoraClient.MODID,
    name = NexoraClient.NAME,
    version = NexoraClient.VERSION,
    clientSideOnly = true,
    acceptedMinecraftVersions = "[1.8.9]"
)
public class NexoraClient {

    public static final String MODID = "nexora";
    public static final String NAME = "NEXORA CLIENT";
    public static final String VERSION = "1.0.0";

    private static final Logger LOGGER = LogManager.getLogger(NAME);
    private static NexoraClient instance;

    private File gameDir;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private ProfileManager profileManager;

    private KeyBinding openGuiKey;
    private KeyBinding openHudEditorKey;

    public NexoraClient() {
        instance = this;
    }

    public static NexoraClient getInstance() {
        return instance;
    }

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        this.gameDir = event.getModConfigurationDirectory().getParentFile();
        LOGGER.info("[NEXORA] Initializing NEXORA CLIENT v" + VERSION + " for Forge 1.8.9 (PojavLauncher GL4ES Safe Mode)");
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        this.moduleManager = new ModuleManager();
        this.moduleManager.init();

        this.configManager = new ConfigManager(gameDir, moduleManager);
        this.profileManager = new ProfileManager(configManager);
        this.configManager.loadProfile(profileManager.getActiveProfile());

        this.openGuiKey = new KeyBinding("Open NEXORA Menu", Keyboard.KEY_RSHIFT, NAME);
        this.openHudEditorKey = new KeyBinding("Open NEXORA HUD Editor", Keyboard.KEY_H, NAME);
        ClientRegistry.registerKeyBinding(openGuiKey);
        ClientRegistry.registerKeyBinding(openHudEditorKey);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(this.moduleManager);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (configManager != null && profileManager != null) {
                    configManager.saveProfileSync(profileManager.getActiveProfile());
                }
            }
        }, "Nexora-Config-Shutdown-Hook"));

        LOGGER.info("[NEXORA] Successfully initialized " + moduleManager.getAllModules().size() + " core modules (" + moduleManager.getHudModules().size() + " HUD widgets).");
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) {
            return;
        }
        if (openGuiKey.isPressed()) {
            mc.displayGuiScreen(new GuiNexoraMain());
            return;
        }
        if (openHudEditorKey.isPressed()) {
            mc.displayGuiScreen(new GuiHudEditor());
            return;
        }
        if (Keyboard.getEventKeyState()) {
            int key = Keyboard.getEventKey();
            if (key <= 0) {
                return;
            }
            List<Module> all = moduleManager.getAllModules();
            for (int i = 0; i < all.size(); i++) {
                Module m = all.get(i);
                // Hold-to-use modules (freelook, zoom) are polled directly in camera/FOV events
                if ("freelook".equals(m.getId()) || "zoom".equals(m.getId())) {
                    continue;
                }
                if (m.getKeyCode() == key) {
                    m.toggle();
                    configManager.saveProfileAsync(profileManager.getActiveProfile());
                }
            }
        }
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }
}
