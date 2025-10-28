package de.unknowncity.papertemplateplugin;

import de.unknowncity.astralib.common.message.lang.Localization;
import de.unknowncity.astralib.common.service.ServiceRegistry;
import de.unknowncity.astralib.paper.api.hook.defaulthooks.PlaceholderApiHook;
import de.unknowncity.astralib.paper.api.message.PaperMessenger;
import de.unknowncity.astralib.paper.api.plugin.PaperAstraPlugin;
import de.unknowncity.papertemplateplugin.configuration.TemplateConfiguration;

public class PaperTemplatePlugin extends PaperAstraPlugin {
    private ServiceRegistry<PaperTemplatePlugin> serviceRegistry;
    private TemplateConfiguration configuration;
    private PaperMessenger messenger;

    @Override
    public void onPluginEnable() {
        initConfiguration();

        initializeMessenger();
    }

    @Override
    public void onPluginDisable() {

    }

    public void initConfiguration() {
        var configOpt = TemplateConfiguration.loadFromFile(TemplateConfiguration.class);
        this.configuration = configOpt.orElseGet(TemplateConfiguration::new);
        this.configuration.save();
    }

    private void initializeMessenger() {
        var defaultLang = languageService.getDefaultLanguage();

        var localization = Localization.builder(getDataPath().resolve("lang")).buildAndLoad();

        this.messenger = PaperMessenger.builder(localization, getPluginMeta())
                .withDefaultLanguage(defaultLang)
                .withLanguageService(languageService)
                .withPlaceHolderAPI(hookRegistry.getRegistered(PlaceholderApiHook.class))
                .build();
    }

    public ServiceRegistry<PaperTemplatePlugin> serviceRegistry() {
        return serviceRegistry;
    }

    public PaperMessenger messenger() {
        return messenger;
    }
}