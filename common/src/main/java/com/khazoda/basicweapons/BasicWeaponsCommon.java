package com.khazoda.basicweapons;

import com.khazoda.basicweapons.materialpack.MaterialPackLoader;
import com.khazoda.basicweapons.platform.Services;

public class BasicWeaponsCommon {
  // Other Mods
  public final static boolean bettercombat_mod_loaded = Services.PLATFORM.isModLoaded("bettercombat");
  public final static boolean bronze_mod_loaded = Services.PLATFORM.isModLoaded("bronze");

  public static void init() {
    BasicWeaponsConfig.init();
    MaterialPackLoader.loadPacks();

    if (Services.PLATFORM.isModLoaded("basicweapons"))
      Constants.LOG.info("- Basic Weapons Loaded -");
  }

  public static void postRegister() {
    if (!Services.PLATFORM.registerFurnaceFuels()) {
      Constants.LOG.info("Wooden weapons not registered correctly as furnace fuels. Please report this on the GitHub repository.");
    }
  }
}