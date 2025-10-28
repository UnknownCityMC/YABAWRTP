package de.unknowncity.yabawrtp.configuration;

import de.unknowncity.astralib.common.configuration.YamlAstraConfiguration;
import de.unknowncity.astralib.common.configuration.annotation.Config;
import de.unknowncity.astralib.common.configuration.setting.defaults.ModernDataBaseSetting;
import de.unknowncity.astralib.libs.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@Config(targetFile = "plugins/UC-YABAWRTP/config.yml")
public class RtpConfiguration extends YamlAstraConfiguration {

    @JsonProperty("default")
    private RtpSettings defaultSettings = new RtpSettings();
    @JsonProperty
    private Map<String, RtpSettings> worlds = Map.of(
            "world", new RtpSettings()
    );
    @JsonProperty
    private int maxTries = 100;
    @JsonProperty
    private int keepInCache = 5;

    public RtpConfiguration() {

    }

    public RtpSettings defaultSettings() {
        return defaultSettings;
    }

    public RtpSettings worldSettings(String world) {
        return worlds.getOrDefault(world, defaultSettings);
    }

    public int maxTries() {
        return maxTries;
    }

    public Map<String, RtpSettings> worlds() {
        return worlds;
    }

    public int keepInCache() {
        return keepInCache;
    }
}
