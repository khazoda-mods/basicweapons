package com.khazoda.basicweapons.registry;

import com.khazoda.basicweapons.material.ConditionalToolMaterials;
import com.khazoda.basicweapons.materialpack.MaterialPackLoader;
import com.khazoda.basicweapons.struct.WeaponType;
import com.khazoda.core.reg.KhazReg.Entry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.khazoda.basicweapons.BasicWeaponsCommon.bronze_mod_loaded;
import static com.khazoda.basicweapons.struct.WeaponType.BasicWeaponType;
import static com.khazoda.basicweapons.struct.WeaponType.WeaponTypeInterface;

public class WeaponRegistry {
  /* Raw suppliers for registration */
  private static final Map<String, Supplier<Item>> ITEMS = new LinkedHashMap<>();
  private static final Map<WeaponTypeInterface, List<Supplier<Item>>> ALL_ITEMS_BY_TYPE = new HashMap<>();
  private static final Map<WeaponTypeInterface, List<Supplier<Item>>> BUILTIN_ITEMS_BY_TYPE = new HashMap<>();
  private static final Map<WeaponTypeInterface, List<Supplier<Item>>> MATERIALPACK_ITEMS_BY_TYPE = new HashMap<>();
  private static final Map<ToolMaterial, List<Supplier<Item>>> ITEMS_BY_MATERIAL = new HashMap<>();

  /* Cached maps for runtime retrieval */
  private static final Map<ToolMaterial, List<Item>> CACHED_MATERIAL_ITEMS = new ConcurrentHashMap<>();
  private static final Map<ITEMS_BY_TYPE, Map<WeaponTypeInterface, List<Item>>> CACHED_TYPED_ITEMS = new ConcurrentHashMap<>();

  public static final List<MaterialEntry> VANILLA_MATERIALS = Arrays.asList(
      new MaterialEntry(ToolMaterial.WOOD, "wooden"),
      new MaterialEntry(ToolMaterial.STONE, "stone"),
      new MaterialEntry(ToolMaterial.COPPER, "copper"),
      new MaterialEntry(ToolMaterial.IRON, "iron"),
      new MaterialEntry(ToolMaterial.GOLD, "golden"),
      new MaterialEntry(ToolMaterial.DIAMOND, "diamond"),
      new MaterialEntry(ToolMaterial.NETHERITE, "netherite", Item.Properties::fireResistant)
  );
  public static final List<MaterialEntry> COMPAT_MATERIALS = Arrays.asList(
      new MaterialEntry(ConditionalToolMaterials.BRONZE, "bronze"),
      new MaterialEntry(ConditionalToolMaterials.TIN, "tin")
  );

  public static void init() {
    boolean runningDatagen = isDataGenerationEnabled();
    for (MaterialEntry material : VANILLA_MATERIALS) {
      registerAllWeaponsForMaterial(material);
    }
    if (bronze_mod_loaded || runningDatagen) {
      for (MaterialEntry material : COMPAT_MATERIALS) {
        registerAllWeaponsForMaterial(material);
      }
    }
    if (runningDatagen) {
      registerDatagenPlaceholder("bronze:bronze_nugget");
      registerDatagenPlaceholder("bronze:tin_nugget");
    }
  }

  public static void registerWeaponForMaterial(WeaponTypeInterface type, MaterialEntry material) {
    String itemId = material.prefix() + "_" + type.getId();
    float damageModifier = WeaponType.getDamageModifier(type, material.material());
    float speedModifier = WeaponType.getSpeedModifier(type, material.material());
    float reachModifier = WeaponType.getReachModifier(type, material.material());

    Entry<Item> itemSupplier = MainRegistry.REG.item(itemId, (key, properties) ->
        type.create(material.material, damageModifier, speedModifier, reachModifier, material.settingsModifier().apply(properties))
    );

    ITEMS.put(itemId, itemSupplier);

    if (VANILLA_MATERIALS.contains(material) || COMPAT_MATERIALS.contains(material)) {
      BUILTIN_ITEMS_BY_TYPE.computeIfAbsent(type, k -> new ArrayList<>()).add(itemSupplier);
    } else {
      MATERIALPACK_ITEMS_BY_TYPE.computeIfAbsent(type, k -> new ArrayList<>()).add(itemSupplier);
    }
    ALL_ITEMS_BY_TYPE.computeIfAbsent(type, k -> new ArrayList<>()).add(itemSupplier);
    ITEMS_BY_MATERIAL.computeIfAbsent(material.material(), k -> new ArrayList<>()).add(itemSupplier);
  }

  public static void registerAllWeaponsForMaterial(MaterialEntry material) {
    for (BasicWeaponType type : BasicWeaponType.values()) {
      registerWeaponForMaterial(type, material);
    }
  }

  /* Register weapons from string of material name (used for material packs) */
  public static void registerAllWeaponsForMaterialPackMaterial(String materialName) {
    ToolMaterial material = MaterialPackLoader.getMaterial(materialName);
    MaterialEntry materialEntry = new MaterialEntry(material, materialName);

    // Register BasicWeaponType weapons (dagger, hammer, club, etc.) only if textures exist
    for (WeaponType.BasicWeaponType type : WeaponType.BasicWeaponType.values()) {
      if (MaterialPackLoader.hasTextureForWeaponType(materialName, type.getId())) {
        registerWeaponForMaterial(type, materialEntry);
      }
    }

    // Register VanillaWeaponType weapons (sword, axe, spear) only if textures exist
    for (WeaponType.VanillaWeaponType type : WeaponType.VanillaWeaponType.values()) {
      if (MaterialPackLoader.hasTextureForWeaponType(materialName, type.getId())) {
        registerWeaponForMaterial(type, materialEntry);
      }
    }
  }

  /* ITEMS_BY_TYPE retrieval options for tab registry */
  public enum ITEMS_BY_TYPE {
    ALL,
    BUILTIN,
    MATERIALPACK
  }

  /* Retrieve items by type (built-in/materialpack) */
  public static List<Item> getItemsByType(ITEMS_BY_TYPE selection, WeaponTypeInterface type) {
    return CACHED_TYPED_ITEMS.computeIfAbsent(selection, k -> new ConcurrentHashMap<>()).computeIfAbsent(type, t -> {
      List<Supplier<Item>> suppliers = switch (selection) {
        case BUILTIN -> BUILTIN_ITEMS_BY_TYPE.getOrDefault(t, Collections.emptyList());
        case MATERIALPACK -> MATERIALPACK_ITEMS_BY_TYPE.getOrDefault(t, Collections.emptyList());
        default -> ALL_ITEMS_BY_TYPE.getOrDefault(t, Collections.emptyList());
      };
      return suppliers.stream().map(Supplier::get).filter(Objects::nonNull).toList();
    });
  }

  /* Retrieve items by material */
  public static List<Item> getItemsByMaterial(ToolMaterial material) {
    return CACHED_MATERIAL_ITEMS.computeIfAbsent(material, mat -> ITEMS_BY_MATERIAL.getOrDefault(mat, Collections.emptyList()).stream().map(Supplier::get).filter(Objects::nonNull).toList());
  }

  public static List<Item> getMaterialPackItems() {
    return MATERIALPACK_ITEMS_BY_TYPE.values().stream().flatMap(Collection::stream).distinct().map(Supplier::get).filter(Objects::nonNull).toList();
  }

  public static boolean hasMaterialPackItems() {
    return MATERIALPACK_ITEMS_BY_TYPE.values().stream().anyMatch(items -> !items.isEmpty());
  }

  public record MaterialEntry(ToolMaterial material, String prefix,
                              Function<Item.Properties, Item.Properties> settingsModifier) {
    public MaterialEntry(ToolMaterial material, String prefix) {
      this(material, prefix, settings -> settings);
    }
  }

  // datagen
  private static boolean isDataGenerationEnabled() {
    return System.getProperty("fabric-api.datagen") != null;
  }

  // dummy entries for tin & bronze nuggets so that datagen can work
  private static void registerDatagenPlaceholder(String id) {
    Identifier identifier = Identifier.parse(id);
    if (BuiltInRegistries.ITEM.containsKey(identifier)) return;
    ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, identifier);
    Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
  }
}