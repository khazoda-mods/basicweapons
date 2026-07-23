package com.khazoda.basicweapons.registry;

import com.khazoda.basicweapons.struct.WeaponType;
import com.khazoda.core.reg.KhazReg.Entry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Consumer;

public final class TabRegistry {
  public static final Entry<CreativeModeTab> BUILTIN_BASIC_WEAPONS_TAB = MainRegistry.REG.tab(
      "main", () -> new ItemStack(WeaponRegistry.getItemsByMaterial(ToolMaterial.IRON).getFirst()));

  public static final Entry<CreativeModeTab> MATERIALPACK_BASIC_WEAPONS_TAB = !WeaponRegistry.hasMaterialPackItems()
      ? null
      : MainRegistry.REG.tab("materialpack", () -> new ItemStack(WeaponRegistry.getMaterialPackItems().getFirst()));

  private TabRegistry() {
  }

  public static void init() {
  }

  public static void addBuiltinItems(Consumer<ItemLike> output) {
    addItems(WeaponRegistry.ITEMS_BY_TYPE.BUILTIN, output);
  }

  public static void addMaterialPackItems(Consumer<ItemLike> output) {
    addItems(WeaponRegistry.ITEMS_BY_TYPE.MATERIALPACK, output);
  }

  private static void addItems(WeaponRegistry.ITEMS_BY_TYPE selection, Consumer<ItemLike> output) {
    for (WeaponType.VanillaWeaponType type : WeaponType.VanillaWeaponType.values()) {
      acceptAll(WeaponRegistry.getItemsByType(selection, type), output);
    }
    for (WeaponType.BasicWeaponType type : WeaponType.BasicWeaponType.values()) {
      if (type != WeaponType.BasicWeaponType.SPEAR) {
        acceptAll(WeaponRegistry.getItemsByType(selection, type), output);
      }
    }
  }

  private static void acceptAll(List<Item> items, Consumer<ItemLike> output) {
    items.forEach(output);
  }
}