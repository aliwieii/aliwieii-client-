package com.nexora.client.profiles;

import com.nexora.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NEXORA CLIENT — Multi-Profile Manager.
 * Ships with Practice, Bed Wars, General PvP, Performance, and Custom profiles.
 * Protects the active profile from accidental deletion without fallback selection.
 */
public final class ProfileManager {

    private final List<String> profiles = new ArrayList<String>(Arrays.asList(
        "Practice",
        "Bed Wars",
        "General PvP",
        "Performance",
        "Custom"
    ));

    private final ConfigManager configManager;
    private String activeProfile = "Bed Wars";

    public ProfileManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public List<String> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public void switchProfile(String targetProfile) {
        if (targetProfile == null || !profiles.contains(targetProfile)) {
            return;
        }
        configManager.saveProfileAsync(activeProfile);
        this.activeProfile = targetProfile;
        configManager.loadProfile(targetProfile);
    }

    public boolean createProfile(String newName) {
        if (newName == null) return false;
        String trimmed = newName.trim();
        if (trimmed.isEmpty() || profiles.contains(trimmed)) {
            return false;
        }
        profiles.add(trimmed);
        switchProfile(trimmed);
        return true;
    }

    public boolean deleteProfile(String nameToDelete) {
        if (profiles.size() <= 1 || !profiles.contains(nameToDelete)) {
            return false;
        }
        if (nameToDelete.equals(activeProfile)) {
            for (String candidate : profiles) {
                if (!candidate.equals(nameToDelete)) {
                    switchProfile(candidate);
                    break;
                }
            }
        }
        return profiles.remove(nameToDelete);
    }
}
