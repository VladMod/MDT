package com.macedamage;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Возвращаем null — мод пока не имеет настроек,
        // но зарегистрирован в Mod Menu с именем и описанием из fabric.mod.json
        return null;
    }
}
