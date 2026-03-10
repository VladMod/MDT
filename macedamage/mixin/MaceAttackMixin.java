package com.macedamage.mixin;

import com.macedamage.MaceDamageMod;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public class MaceAttackMixin {

    /**
     * Перехватываем атаку игрока.
     * Если в руке булава — вычисляем базовый урон + бонус от падения и сохраняем.
     */
    @Inject(method = "attack", at = @At("HEAD"))
    private void onMaceAttack(Entity target, CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ItemStack mainHand = player.getMainHandStack();

        // Проверяем, что в руке именно булава (Mace)
        if (!mainHand.isOf(Items.MACE)) {
            return;
        }

        if (!(target instanceof LivingEntity)) {
            return;
        }

        World world = player.getWorld();

        // Базовый урон булавы = 6.0 (атрибут attack_damage у mace = 6)
        float baseDamage = 6.0f;

        // Бонус от падения: +3 урона за каждый блок падения (первые 3 блока),
        // далее +1 за каждый следующий (механика 1.21)
        float fallDistance = player.fallDistance;
        float fallBonus = calculateFallBonus(fallDistance);

        float totalDamage = baseDamage + fallBonus;

        // Бонус от зачарования Density (+1 за уровень за каждый блок падения, макс 5 ур.)
        // Density: +densityLevel урона на каждый блок падения
        // Breach: уменьшает броню на 15% * уровень (не меняет урон, меняет пробитие)
        // Учитываем Density через EnchantmentHelper
        // В 1.21 EnchantmentHelper.getMaceSmashDamageBonus — внутренняя механика,
        // поэтому считаем вручную через уровень зачарования
        int densityLevel = getDensityLevel(mainHand, world);
        float densityBonus = densityLevel * fallDistance;
        totalDamage += densityBonus;

        MaceDamageMod.lastMaceDamage = totalDamage;
        MaceDamageMod.lastFallBonus = fallBonus + densityBonus;
        MaceDamageMod.lastHitTime = System.currentTimeMillis();

        MaceDamageMod.LOGGER.info(
            "[MaceDamage] Hit! Base: {}, Fall bonus: {} ({}блоков), Density bonus: {}, TOTAL: {}",
            baseDamage, fallBonus, fallDistance, densityBonus, totalDamage
        );
    }

    /**
     * Механика бонуса от падения булавы в 1.21:
     * - Первые 3 блока падения: +3 урона за блок
     * - Каждый последующий блок: +1 урон
     */
    private float calculateFallBonus(float fallDistance) {
        if (fallDistance <= 0) return 0f;

        float bonus = 0f;
        float firstPhase = Math.min(fallDistance, 3.0f);
        bonus += firstPhase * 3.0f;

        if (fallDistance > 3.0f) {
            bonus += (fallDistance - 3.0f) * 1.0f;
        }

        return bonus;
    }

    /**
     * Получаем уровень зачарования Density на булаве.
     * В Minecraft 1.21 используется registry-based система зачарований.
     */
    private int getDensityLevel(ItemStack stack, World world) {
        try {
            // Используем EnchantmentHelper для получения уровня Density
            // Density = net.minecraft.enchantment.Enchantments.DENSITY (1.21)
            var registryManager = world.getRegistryManager();
            var enchantmentRegistry = registryManager.get(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
            var densityEntry = enchantmentRegistry.getEntry(
                net.minecraft.util.Identifier.of("minecraft", "density")
            );
            if (densityEntry.isPresent()) {
                return EnchantmentHelper.getLevel(densityEntry.get(), stack);
            }
        } catch (Exception e) {
            // Зачарование не найдено — игнорируем
        }
        return 0;
    }
}
