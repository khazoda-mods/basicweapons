package com.khazoda.basicweapons;

import com.khazoda.basicweapons.registry.MainRegistry;
import com.khazoda.basicweapons.registry.TabRegistry;
import com.khazoda.core.config.KhazConfigSyncNeoForge;
import com.khazoda.core.reg.KhazRegNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(Constants.MOD_ID)
public class BasicWeaponsNeoForge {
  public BasicWeaponsNeoForge(IEventBus eventBus) {
    BasicWeaponsCommon.init();
    KhazConfigSyncNeoForge.registerPayloadHandlers(eventBus, Constants.CONFIG_SYNC);
    KhazRegNeoForge.init(eventBus, MainRegistry::init);
    BasicWeaponsCommon.postRegister();
    eventBus.addListener(this::onBuildCreativeModeTabContents);
  }

  private void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
    if (TabRegistry.BUILTIN_BASIC_WEAPONS_TAB.key().equals(event.getTabKey())) {
      TabRegistry.addBuiltinItems(event::accept);
    } else if (TabRegistry.MATERIALPACK_BASIC_WEAPONS_TAB != null && TabRegistry.MATERIALPACK_BASIC_WEAPONS_TAB.key().equals(event.getTabKey())) {
      TabRegistry.addMaterialPackItems(event::accept);
    }
  }
}