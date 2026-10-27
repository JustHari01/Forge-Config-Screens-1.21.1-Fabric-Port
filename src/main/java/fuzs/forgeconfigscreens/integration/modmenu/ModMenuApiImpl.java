package fuzs.forgeconfigscreens.integration.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fuzs.forgeconfigscreens.ForgeConfigScreens;
import fuzs.forgeconfigscreens.client.helper.ConfigScreenHelper;
import net.neoforged.fml.config.ModConfigs;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ModMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreenHelper.createConfigScreen(ForgeConfigScreens.MOD_ID).orElse(screen -> null)::apply;
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new HashMap<>();
        ModConfigs.getFileMap().values().stream()
                .map(config -> config.modConfig)
                .filter(Objects::nonNull)
                .map(ModConfig::getModId)
                .distinct().forEach(modId -> {
            ConfigScreenHelper.createConfigScreen(modId).ifPresent(screenFactory -> {
                factories.put(modId, screenFactory::apply);
            });
        });
        return factories;
    }
}
