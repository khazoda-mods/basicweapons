package com.khazoda.basicweapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class CustomAxeItem extends Item {
  public CustomAxeItem(ToolMaterial material, float attackDamage, float attackSpeed, double reach, Properties properties) {
    super(properties
        .axe(material, attackDamage, attackSpeed)
        .attributes(BasicWeaponItem.createAttributes(material, attackDamage, attackSpeed, reach)));
  }
}