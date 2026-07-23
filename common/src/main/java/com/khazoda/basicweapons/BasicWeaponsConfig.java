package com.khazoda.basicweapons;

import com.khazoda.basicweapons.platform.Services;
import com.khazoda.core.config.KhazConfig;

public final class BasicWeaponsConfig {
  public static final KhazConfig.Entry<Boolean> ENABLE_LOOT_TABLE_POPULATION = KhazConfig.bool("enable_loot_table_population", true, "Adds basic weapons to vanilla loot tables.");
  public static final KhazConfig.Entry<Boolean> ENABLE_MOB_WEAPON_EQUIPMENT = KhazConfig.bool("enable_mob_weapon_equipment", true, "Allows adult zombies and piglins to spawn holding basic weapons.");

  public static final KhazConfig CONFIG = KhazConfig.of(Constants.MOD_NAME, Constants.MOD_ID, Services.PLATFORM.getConfigDirectory(), ENABLE_LOOT_TABLE_POPULATION, ENABLE_MOB_WEAPON_EQUIPMENT);

  private BasicWeaponsConfig() {
  }

  public static void init() {
    CONFIG.load();
    Services.PLATFORM.registerServerConfigSync(CONFIG);
  }

  public static boolean lootTablePopulationEnabled() {
    return CONFIG.get(ENABLE_LOOT_TABLE_POPULATION);
  }

  public static boolean mobWeaponEquipmentEnabled() {
    return CONFIG.get(ENABLE_MOB_WEAPON_EQUIPMENT);
  }
}