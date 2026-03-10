package com.macedamage;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class MaceDamageMod implements ClientModInitializer {

    public static final String MOD_ID = "macedamage";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Shared state: последний рассчитанный урон от булавы
    public static float lastMaceDamage = 0f;
    public static float lastFallBonus = 0f;
    public static long lastHitTime = 0L;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[MaceDamage] Mace Damage mod loaded!");
        MaceDamageHud.register();
    }
}
