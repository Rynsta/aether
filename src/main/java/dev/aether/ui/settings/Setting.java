package dev.aether.ui.settings;

// implementations hold typed getter/setter references to AetherConfig entries
public interface Setting {
    String getName();
    default String getRawName() {
        return getName();
    }
    SettingType getType();
    boolean isVisible();
}
