package com.khazoda.basicweapons;

import com.khazoda.basicweapons.fabric.FabricEventManager;
import com.khazoda.basicweapons.registry.FabricLootTableModifier;
import com.khazoda.basicweapons.registry.MainRegistry;
import com.khazoda.basicweapons.registry.TabRegistry;
import com.khazoda.core.reg.KhazRegFabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public class BasicWeaponsFabric implements ModInitializer {
  @Override
  public void onInitialize() {
    BasicWeaponsCommon.init();
    KhazRegFabric.init(MainRegistry::init);
    BasicWeaponsCommon.postRegister();

    CreativeModeTabEvents.modifyOutputEvent(TabRegistry.BUILTIN_BASIC_WEAPONS_TAB.key()).register(output -> TabRegistry.addBuiltinItems(output::accept));
    if (TabRegistry.MATERIALPACK_BASIC_WEAPONS_TAB != null) {
      CreativeModeTabEvents.modifyOutputEvent(TabRegistry.MATERIALPACK_BASIC_WEAPONS_TAB.key()).register(output -> TabRegistry.addMaterialPackItems(output::accept));
    }

    FabricEventManager.init();
    FabricLootTableModifier.init();
  }
}