package dev.aether.ui.settings;

import dev.aether.util.AetherLang;

import java.util.Map;

final class SettingDescriptionCatalog {
    private static final Map<String, String> EXPLICIT = Map.ofEntries(
            Map.entry("Pest Threshold", "Number of pests required before Pest Destroyer starts automatically."),
            Map.entry("Leave One Pest Alive", "Preserves one pest on selected plots instead of fully clearing them."),
            Map.entry("AOTV Between Distant Pests", "Allows Pest Destroyer to use an Aspect of the Void between distant pests."),
            Map.entry("Next Pest Turn Speed", "Controls how quickly the camera turns when handing off to the next pest or aiming an AOTV hop."),
            Map.entry("Optimized Route ESP", "Draws the planned Pest Destroyer route from the current target through nearby pests."),
            Map.entry("Optimized Route Color", "Sets the color used for the optimized Pest Destroyer route."),
            Map.entry("Highlight", "Draws a highlight around detected pests."),
            Map.entry("Tracer", "Draws a screen-space line toward detected pests."),
            Map.entry("UI Scale", "Changes the overall size of the Aether menu."),
            Map.entry("Text Scale", "Changes text size without changing the full menu scale."),
            Map.entry("Limit FPS", "Caps Minecraft's frame rate while the related condition is active."),
            Map.entry("Auto Reconnect", "Automatically attempts to reconnect after supported disconnects."),
            Map.entry("Edit HUD Layout", "Opens the HUD editor so Aether overlays can be moved and resized."),
            Map.entry("Language", "Chooses the language used by the Aether interface."),
            Map.entry("Save Preset", "Saves the current values as a reusable preset."),
            Map.entry("Reset Session", "Clears the current session statistics."),
            Map.entry("Webhook URL", "Sets the notification webhook destination."),
            Map.entry("Bot Token", "Sets the integration token. Keep this value private."),
            Map.entry("Pathfinder Max Jump Height", "Sets the maximum vertical step the pathfinder may plan through."),
            Map.entry("Warp Grace Period", "Allows position checks to settle briefly after a warp.")
    );

    private SettingDescriptionCatalog() {
    }

    static String describe(Setting setting) {
        if (setting == null) {
            return "";
        }
        String raw = setting.getRawName() == null ? setting.getName() : setting.getRawName();
        String explicit = EXPLICIT.get(raw);
        return AetherLang.localize(explicit != null ? explicit : fallback(setting.getType(), raw));
    }

    private static String fallback(SettingType type, String raw) {
        String subject = raw == null || raw.isBlank() ? "this setting" : raw;
        return switch (type) {
            case TOGGLE -> "Turns " + lowerFirst(subject) + " on or off.";
            case SLIDER -> "Adjusts " + lowerFirst(subject) + ".";
            case RANGE_SLIDER -> "Sets the minimum and maximum values for " + lowerFirst(subject) + ".";
            case DROPDOWN -> "Chooses the mode used for " + lowerFirst(subject) + ".";
            case DROPDOWN_LIST -> "Sets the ordered choices used for " + lowerFirst(subject) + ".";
            case MULTI_DROPDOWN -> "Selects the entries included in " + lowerFirst(subject) + ".";
            case LIST -> "Manages the saved entries used by " + lowerFirst(subject) + ".";
            case TEXT -> "Sets the text used for " + lowerFirst(subject) + ".";
            case COLOR -> "Sets the color used for " + lowerFirst(subject) + ".";
            case POSITION -> "Sets the in-world position used for " + lowerFirst(subject) + ".";
            case KEYBIND -> "Changes the keyboard key used for " + lowerFirst(subject) + ".";
            case ACTION -> "Runs the “" + subject + "” action immediately.";
            case INFO -> "Displays current information for " + lowerFirst(subject) + ".";
            case SECTION -> "Groups settings related to " + lowerFirst(subject) + ".";
        };
    }

    private static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return "this setting";
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
