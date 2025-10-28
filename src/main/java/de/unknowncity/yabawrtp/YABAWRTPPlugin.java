package de.unknowncity.yabawrtp;

import de.unknowncity.astralib.common.message.lang.Localization;
import de.unknowncity.astralib.common.service.ServiceRegistry;
import de.unknowncity.astralib.paper.api.hook.defaulthooks.PlaceholderApiHook;
import de.unknowncity.astralib.paper.api.message.PaperMessenger;
import de.unknowncity.astralib.paper.api.plugin.PaperAstraPlugin;
import de.unknowncity.yabawrtp.command.RtpCommand;
import de.unknowncity.yabawrtp.configuration.RtpConfiguration;

import java.nio.file.Path;

public class YABAWRTPPlugin extends PaperAstraPlugin {
    private ServiceRegistry<YABAWRTPPlugin> serviceRegistry;
    private RtpConfiguration configuration;
    private PaperMessenger messenger;
    private AbstractDelegateLocationFactoryBean abstractDelegateLocationFactoryBean;

    @Override
    public void onPluginEnable() {
        initConfiguration();

        initializeMessenger();
        this.abstractDelegateLocationFactoryBean = new AbstractDelegateLocationFactoryBean(this);
        this.abstractDelegateLocationFactoryBean.warmupCache();
        new RtpCommand(this).apply(commandManager());
    }

    @Override
    public void onPluginDisable() {

    }

    public void initConfiguration() {
        var configOpt = RtpConfiguration.loadFromFile(RtpConfiguration.class);
        this.configuration = configOpt.orElseGet(RtpConfiguration::new);
        this.configuration.save();
    }

    private void initializeMessenger() {
        this.saveDefaultResource("lang/de_DE.yml", Path.of("lang/de_DE.yml"));
        var defaultLang = languageService.getDefaultLanguage();

        var localization = Localization.builder(getDataPath().resolve("lang")).buildAndLoad();

        this.messenger = PaperMessenger.builder(localization, getPluginMeta())
                .withDefaultLanguage(defaultLang)
                .withLanguageService(languageService)
                .withPlaceHolderAPI(hookRegistry.getRegistered(PlaceholderApiHook.class))
                .build();
    }

    public ServiceRegistry<YABAWRTPPlugin> serviceRegistry() {
        return serviceRegistry;
    }

    public PaperMessenger messenger() {
        return messenger;
    }

    public RtpConfiguration configuration() {
        return configuration;
    }

    public AbstractDelegateLocationFactoryBean abstractDelegateLocationFactoryBean() {
        return abstractDelegateLocationFactoryBean;
    }
}