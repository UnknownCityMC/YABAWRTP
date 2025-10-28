package de.unknowncity.papertemplateplugin.configuration;

import de.unknowncity.astralib.common.configuration.YamlAstraConfiguration;
import de.unknowncity.astralib.common.configuration.setting.defaults.ModernDataBaseSetting;
import de.unknowncity.astralib.libs.com.fasterxml.jackson.annotation.JsonProperty;

public class TemplateConfiguration extends YamlAstraConfiguration {

    @JsonProperty
    private ModernDataBaseSetting databaseSetting = new ModernDataBaseSetting();


    public ModernDataBaseSetting databaseSetting() {
        return databaseSetting;
    }
}
