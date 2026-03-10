package com.macedamage;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
public class MaceDamageHud implements HudRenderCallback {

    // Время отображения HUD после удара (в миллисекундах)
    private static final long DISPLAY_DURATION_MS = 3000L;

    // Время fade-out (последние 500 мс)
    private static final long FADE_DURATION_MS = 500L;

    public static void register() {
        HudRenderCallback.EVENT.register(new MaceDamageHud());
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        // Показываем только если в руке булава
        if (!client.player.getMainHandStack().isOf(Items.MACE)) return;

        long now = System.currentTimeMillis();
        long elapsed = now - MaceDamageMod.lastHitTime;

        // Определяем прозрачность (fade-out в конце)
        int alpha;
        if (MaceDamageMod.lastHitTime == 0) {
            // Ещё не было удара — показываем предпросмотр урона по цели прицела
            alpha = 180;
        } else if (elapsed < DISPLAY_DURATION_MS - FADE_DURATION_MS) {
            alpha = 255;
        } else if (elapsed < DISPLAY_DURATION_MS) {
            float fadeProgress = (float)(elapsed - (DISPLAY_DURATION_MS - FADE_DURATION_MS)) / FADE_DURATION_MS;
            alpha = (int)(255 * (1.0f - fadeProgress));
        } else {
            alpha = 0;
        }

        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();

        // Позиция: чуть выше центра экрана (над прицелом)
        int x = screenWidth / 2;
        int y = screenHeight / 2 - 30;

        // Рассчитываем предпросмотр (текущая высота падения)
        float currentFall = client.player.fallDistance;
        float previewBase = 6.0f;
        float previewFallBonus = calculateFallBonus(currentFall);
        float previewTotal = previewBase + previewFallBonus;

        // Если был недавний удар — показываем его урон
        boolean showLastHit = MaceDamageMod.lastHitTime > 0 && elapsed < DISPLAY_DURATION_MS;

        if (showLastHit && alpha > 0) {
            // Показываем нанесённый урон после удара
            String dmgText = String.format("⚔ Урон: %.1f (%+.1f от падения)",
                MaceDamageMod.lastMaceDamage, MaceDamageMod.lastFallBonus);

            int color = packColor(alpha, 255, 80, 80); // красный
            drawContext.drawCenteredTextWithShadow(
                client.textRenderer, dmgText, x, y, color
            );
        } else {
            // Предпросмотр текущего урона (пока игрок летит/стоит)
            if (currentFall > 0.5f) {
                String previewText = String.format("⚔ ~%.1f урона  (падение: %.1fб)",
                    previewTotal, currentFall);
                int color = packColor(200, 255, 220, 50); // жёлтый
                drawContext.drawCenteredTextWithShadow(
                    client.textRenderer, previewText, x, y, color
                );
            } else {
                // Просто базовый урон булавы
                String baseText = String.format("⚔ Булава: %.1f урона", previewBase);
                int color = packColor(160, 200, 200, 200); // серый
                drawContext.drawCenteredTextWithShadow(
                    client.textRenderer, baseText, x, y, color
                );
            }
        }
    }

    private float calculateFallBonus(float fallDistance) {
        if (fallDistance <= 0) return 0f;
        float bonus = 0f;
        float firstPhase = Math.min(fallDistance, 3.0f);
        bonus += firstPhase * 3.0f;
        if (fallDistance > 3.0f) {
            bonus += (fallDistance - 3.0f);
        }
        return bonus;
    }

    private int packColor(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
