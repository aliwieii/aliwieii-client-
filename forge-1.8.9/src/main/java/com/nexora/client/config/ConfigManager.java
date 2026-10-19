package com.nexora.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexora.client.hud.HudModule;
import com.nexora.client.localization.I18nManager;
import com.nexora.client.modules.Module;
import com.nexora.client.modules.ModuleManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NEXORA CLIENT — Schema-Versioned Central Configuration Manager.
 * Performs disk I/O strictly off the Render Loop via a single background executor,
 * validates values, and recovers gracefully from corrupted JSON files.
 */
public final class ConfigManager {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File configDir;
    private final ModuleManager moduleManager;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private String activeProfileName = "Bed Wars";

    public ConfigManager(File gameDir, ModuleManager moduleManager) {
        this.configDir = new File(gameDir, "nexora");
        if (!this.configDir.exists()) {
            this.configDir.mkdirs();
        }
        this.moduleManager = moduleManager;
    }

    public File getProfileFile(String profileName) {
        String safeName = profileName.replaceAll("[^a-zA-Z0-9_\\- ]", "_").trim();
        if (safeName.isEmpty()) {
            safeName = "Default";
        }
        return new File(configDir, "profile_" + safeName + ".json");
    }

    public void loadProfile(String profileName) {
        this.activeProfileName = profileName;
        File file = getProfileFile(profileName);
        if (!file.exists()) {
            saveProfileAsync(profileName);
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            JsonElement parsed = new JsonParser().parse(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                handleCorruptedConfig(file);
                return;
            }
            JsonObject root = parsed.getAsJsonObject();
            int version = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 1;
            if (version < CURRENT_SCHEMA_VERSION) {
                migrateSchema(root, version);
            }

            if (root.has("language")) {
                String lang = root.get("language").getAsString();
                I18nManager.setCurrentLanguage("en".equalsIgnoreCase(lang) ? I18nManager.Language.EN : I18nManager.Language.AR);
            }

            if (root.has("modules") && root.get("modules").isJsonObject()) {
                JsonObject modsObj = root.getAsJsonObject("modules");
                for (Module m : moduleManager.getAllModules()) {
                    if (modsObj.has(m.getId()) && modsObj.get(m.getId()).isJsonObject()) {
                        JsonObject mObj = modsObj.getAsJsonObject(m.getId());
                        if (mObj.has("enabled")) {
                            m.setEnabled(mObj.get("enabled").getAsBoolean());
                        }
                        if (mObj.has("keyCode")) {
                            m.setKeyCode(mObj.get("keyCode").getAsInt());
                        }
                        if (m instanceof HudModule) {
                            HudModule hm = (HudModule) m;
                            if (mObj.has("x")) hm.setX(mObj.get("x").getAsFloat());
                            if (mObj.has("y")) hm.setY(mObj.get("y").getAsFloat());
                            if (mObj.has("scale")) hm.setScale(mObj.get("scale").getAsFloat());
                            if (mObj.has("opacity")) hm.setOpacity(mObj.get("opacity").getAsFloat());
                            if (mObj.has("accentColor")) hm.setAccentColor(mObj.get("accentColor").getAsInt());
                            if (mObj.has("locked")) hm.setLocked(mObj.get("locked").getAsBoolean());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            handleCorruptedConfig(file);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void migrateSchema(JsonObject root, int oldVersion) {
        root.addProperty("schemaVersion", CURRENT_SCHEMA_VERSION);
    }

    private void handleCorruptedConfig(File corruptedFile) {
        File backup = new File(configDir, corruptedFile.getName() + ".bak");
        corruptedFile.renameTo(backup);
        for (Module m : moduleManager.getAllModules()) {
            m.resetToDefault();
        }
        saveProfileAsync(activeProfileName);
    }

    public void saveProfileAsync(final String profileName) {
        final JsonObject snapshot = serializeCurrentState(profileName);
        final File targetFile = getProfileFile(profileName);
        ioExecutor.submit(new Runnable() {
            @Override
            public void run() {
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8));
                    GSON.toJson(snapshot, writer);
                    writer.flush();
                } catch (Exception ignored) {
                } finally {
                    if (writer != null) {
                        try { writer.close(); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    private JsonObject serializeCurrentState(String profileName) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", CURRENT_SCHEMA_VERSION);
        root.addProperty("profileName", profileName);
        root.addProperty("language", I18nManager.getCurrentLanguage().getCode());

        JsonObject modsObj = new JsonObject();
        for (Module m : moduleManager.getAllModules()) {
            JsonObject mObj = new JsonObject();
            mObj.addProperty("enabled", m.isEnabled());
            mObj.addProperty("keyCode", m.getKeyCode());
            if (m instanceof HudModule) {
                HudModule hm = (HudModule) m;
                mObj.addProperty("x", hm.getX());
                mObj.addProperty("y", hm.getY());
                mObj.addProperty("scale", hm.getScale());
                mObj.addProperty("opacity", hm.getOpacity());
                mObj.addProperty("accentColor", hm.getAccentColor());
                mObj.addProperty("locked", hm.isLocked());
            }
            modsObj.add(m.getId(), mObj);
        }
        root.add("modules", modsObj);
        return root;
    }

    public String getActiveProfileName() {
        return activeProfileName;
    }
}
