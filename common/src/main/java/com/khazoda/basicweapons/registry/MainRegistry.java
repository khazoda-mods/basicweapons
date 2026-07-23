package com.khazoda.basicweapons.registry;

import com.khazoda.basicweapons.Constants;
import com.khazoda.core.reg.KhazReg;

public final class MainRegistry {
  public static final KhazReg REG = new KhazReg(Constants.MOD_ID);
  private static boolean initialized;

  private MainRegistry() {
  }

  public static KhazReg init() {
    if (initialized) return REG;
    initialized = true;

    WeaponRegistry.init();
    EnchantmentRegistry.init();
    TabRegistry.init();
    REG.freeze();
    return REG;
  }
}