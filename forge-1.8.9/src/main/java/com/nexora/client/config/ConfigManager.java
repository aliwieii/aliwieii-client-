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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * Performs atomic disk I/O strictly off the Render Loop via a single background executor,
 * validates coordinate values, and never overwrites a valid profile upon write failure.
 */
public final class ConfigManager {

    public static final int CURRENT_SCHEMA_VERSION = 4;
    private static final Logger LOGGER = LogManager.getLogger("NEXORA-Config");
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
        String safeName = profileName == null ? "Default" : profileName.replaceAll("[^a-zA-Z0-9_\\- ]", "_").trim();
        if (safeName.isEmpty()) {
            safeName = "Default";
        }
        return new File(configDir, "profile_" + safeName + ".json");
    }

    public void loadProfile(String profileName) {
        if (profileName != null && !profileName.trim().isEmpty()) {
            this.activeProfileName = profileName.trim();
        }
        File file = getProfileFile(this.activeProfileName);
        if (!file.exists()) {
            saveProfileAsync(this.activeProfileName);
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

            if (root.has("hurtCamMultiplier")) {
                moduleManager.setHurtCamMultiplier(root.get("hurtCamMultiplier").getAsFloat());
            }
            if (root.has("hitOverlayColor")) {
                moduleManager.setHitOverlayColor(root.get("hitOverlayColor").getAsInt());
            }
            if (root.has("clientThemeColor")) {
                moduleManager.setClientThemeColor(root.get("clientThemeColor").getAsInt());
            }
            if (root.has("performancePreset")) {
                moduleManager.applyPerformancePreset(root.get("performancePreset").getAsInt());
            }
            if (root.has("safeModeActive")) {
                moduleManager.setSafeModeActive(root.get("safeModeActive").getAsBoolean());
            }
            if (root.has("particleMultiplier")) {
                moduleManager.setParticleMultiplier(root.get("particleMultiplier").getAsInt());
            }
            if (root.has("particleBudgetCap")) {
                moduleManager.setParticleBudgetCap(root.get("particleBudgetCap").getAsInt());
            }
            if (root.has("particlePresetColor")) {
                moduleManager.setParticlePresetColor(root.get("particlePresetColor").getAsInt());
            }
            if (root.has("maxBuildHeight")) {
                moduleManager.setMaxBuildHeight(root.get("maxBuildHeight").getAsInt());
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
                            if (mObj.has("locked")) {
                                hm.setLocked(false); // allow coordinate load before locking
                            }
                            if (mObj.has("x")) {
                                float lx = mObj.get("x").getAsFloat();
                                if (!Float.isNaN(lx) && !Float.isInfinite(lx) && lx >= 0.0f && lx < 5000.0f) {
                                    hm.forcePosition(lx, hm.getY());
                                }
                            }
                            if (mObj.has("y")) {
                                float ly = mObj.get("y").getAsFloat();
                                if (!Float.isNaN(ly) && !Float.isInfinite(ly) && ly >= 0.0f && ly < 5000.0f) {
                                    hm.forcePosition(hm.getX(), ly);
                                }
                            }
                            if (mObj.has("scale")) hm.setScale(mObj.get("scale").getAsFloat());
                            if (mObj.has("opacity")) hm.setOpacity(mObj.get("opacity").getAsFloat());
                            if (mObj.has("accentColor")) hm.setAccentColor(mObj.get("accentColor").getAsInt());
                            if (mObj.has("showBackground")) hm.setShowBackground(mObj.get("showBackground").getAsBoolean());
                            if (mObj.has("locked")) hm.setLocked(mObj.get("locked").getAsBoolean());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("[NEXORA] Corrupted config file detected (" + file.getName() + "), recovering defaults.", ex);
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
        File backup = new File(configDir, corruptedFile.getName() + ".corrupt.bak");
        if (backup.exists()) {
            backup.delete();
        }
        corruptedFile.renameTo(backup);
        for (Module m : moduleManager.getAllModules()) {
            m.resetToDefault();
        }
        saveProfileAsync(activeProfileName);
    }

    /**
     * Atomically writes the profile snapshot to a .tmp file first and only replaces
     * the target file if serialization and disk flush succeed completely.
     */
    public void saveProfileAsync(final String profileName) {
        final JsonObject snapshot = serializeCurrentState(profileName);
        final File targetFile = getProfileFile(profileName);
        ioExecutor.submit(new Runnable() {
            @Override
            public void run() {
                saveSnapshotAtomically(snapshot, targetFile);
            }
        });
    }

    public void saveProfileSync(final String profileName) {
        final JsonObject snapshot = serializeCurrentState(profileName);
        final File targetFile = getProfileFile(profileName);
        saveSnapshotAtomically(snapshot, targetFile);
    }

    private void saveSnapshotAtomically(JsonObject snapshot, File targetFile) {
        String jsonText = GSON.toJson(snapshot);
        if (jsonText == null || jsonText.length() < 20) {
            return;
        }
        File tempFile = new File(configDir, targetFile.getName() + ".tmp");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8));
            writer.write(jsonText);
            writer.flush();
            writer.close();
            writer = null;

            if (tempFile.exists() && tempFile.length() > 20) {
                if (targetFile.exists()) {
                    File prevBackup = new File(configDir, targetFile.getName() + ".bak");
                    if (prevBackup.exists()) {
                        prevBackup.delete();
                    }
                    targetFile.renameTo(prevBackup);
                }
                tempFile.renameTo(targetFile);
            }
        } catch (Exception ex) {
            LOGGER.warn("[NEXORA] Failed to save profile atomically: " + targetFile.getName(), ex);
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (Exception ignored) {}
            }
        }
    }

    private JsonObject serializeCurrentState(String profileName) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", CURRENT_SCHEMA_VERSION);
        root.addProperty("profileName", profileName);
        root.addProperty("language", I18nManager.getCurrentLanguage().getCode());
        root.addProperty("hurtCamMultiplier", moduleManager.getHurtCamMultiplier());
        root.addProperty("hitOverlayColor", moduleManager.getHitOverlayColor());
        root.addProperty("clientThemeColor", moduleManager.getClientThemeColor());
        root.addProperty("performancePreset", moduleManager.getPerformancePreset());
        root.addProperty("safeModeActive", moduleManager.isSafeModeActive());
        root.addProperty("particleMultiplier", moduleManager.getParticleMultiplier());
        root.addProperty("particleBudgetCap", moduleManager.getParticleBudgetCap());
        root.addProperty("particlePresetColor", moduleManager.getParticlePresetColor());
        root.addProperty("maxBuildHeight", moduleManager.getMaxBuildHeight());

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
                mObj.addProperty("showBackground", hm.isShowBackground());
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
