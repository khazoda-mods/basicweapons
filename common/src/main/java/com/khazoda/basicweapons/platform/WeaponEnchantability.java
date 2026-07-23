package com.khazoda.basicweapons.platform;

import com.khazoda.basicweapons.utils.AllowDenyPass;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public interface WeaponEnchantability {
  AllowDenyPass getEnchantability(ItemStack itemStack, Holder<Enchantment> enchantment);
}