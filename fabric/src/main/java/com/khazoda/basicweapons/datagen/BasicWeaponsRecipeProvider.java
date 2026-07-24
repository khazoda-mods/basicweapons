package com.khazoda.basicweapons.datagen;

import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.khazoda.basicweapons.registry.TagRegistry;
import com.khazoda.basicweapons.registry.WeaponRegistry;
import com.khazoda.basicweapons.registry.WeaponRegistry.MaterialEntry;
import com.khazoda.basicweapons.struct.WeaponType.BasicWeaponType;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.khazoda.basicweapons.Constants.ID;
import static com.khazoda.basicweapons.struct.WeaponType.BasicWeaponType.*;

public class BasicWeaponsRecipeProvider extends FabricRecipeProvider {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final List<BasicWeaponType> RECIPE_TYPES = List.of(DAGGER, HAMMER, CLUB, PIKE, QUARTERSTAFF, GLAIVE);
  private static final List<Shape> SHAPES = List.of(
      new Shape(DAGGER, "", "#", "$", "/"),
      new Shape(CLUB, "", "#", " $", "$ ", "/ "),
      new Shape(CLUB, "_variant", "#", "$ ", " $", " /"),
      new Shape(HAMMER, "", "#", "$$$", "$/$", " / "),
      new Shape(PIKE, "", "^", "  $", " $ ", "/  "),
      new Shape(QUARTERSTAFF, "", "", "  /", " $ ", "/  "),
      new Shape(GLAIVE, "", "", " $$", "$/ ", "/  ")
  );
  private static final List<Recycling> RECYCLING = List.of(
      new Recycling("copper", "copper", "minecraft:copper_nugget", Condition.NONE),
      new Recycling("iron", "iron", "minecraft:iron_nugget", Condition.NONE),
      new Recycling("golden", "gold", "minecraft:gold_nugget", Condition.NONE),
      new Recycling("bronze", "bronze", "bronze:bronze_nugget", Condition.BRONZE),
      new Recycling("tin", "tin", "bronze:tin_nugget", Condition.BRONZE)
  );

  private final Map<Identifier, ConditionalRecipe> conditionalRecipes = new LinkedHashMap<>();

  public BasicWeaponsRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
    conditionalRecipes.clear();
    return new Generator(registries, output);
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    return super.run(cache).thenCompose(ignored -> appendNeoForgeConditions(cache));
  }

  @Override
  public String getName() {
    return "Basic Weapons Recipe Provider";
  }

  private final class Generator extends RecipeProvider {
    private final HolderGetter<Item> items;
    private final RecipeOutput output;

    private Generator(HolderLookup.Provider registries, RecipeOutput output) {
      super(registries, output);
      this.items = registries.lookupOrThrow(Registries.ITEM);
      this.output = output;
    }

    @Override
    public void buildRecipes() {
      materials().forEach(this::createWeaponRecipes);
      createNetheriteSmithingRecipes();
      createRecyclingRecipes();
    }

    private Stream<RecipeMaterial> materials() {
      return Stream.concat(
          WeaponRegistry.VANILLA_MATERIALS.stream().filter(entry -> entry.material() != ToolMaterial.NETHERITE),
          WeaponRegistry.COMPAT_MATERIALS.stream()
      ).map(entry -> new RecipeMaterial(entry, ingredient(entry.prefix())));
    }

    private Ingredient ingredient(String material) {
      return switch (material) {
        case "wooden" -> Ingredient.of(items.getOrThrow(ItemTags.WOODEN_TOOL_MATERIALS));
        case "stone" -> Ingredient.of(items.getOrThrow(ItemTags.STONE_TOOL_MATERIALS));
        case "copper" -> Ingredient.of(Items.COPPER_INGOT);
        case "iron" -> Ingredient.of(Items.IRON_INGOT);
        case "golden" -> Ingredient.of(Items.GOLD_INGOT);
        case "diamond" -> Ingredient.of(Items.DIAMOND);
        case "bronze" -> Ingredient.of(items.getOrThrow(TagRegistry.BRONZE_INGOTS));
        case "tin" -> Ingredient.of(items.getOrThrow(TagRegistry.TIN_INGOTS));
        default -> throw new IllegalArgumentException("Unsupported recipe material: " + material);
      };
    }

    private void createWeaponRecipes(RecipeMaterial material) {
      for (Shape shape : SHAPES) {
        Item result = weapon(material.id(), shape.type());
        char key = shape.materialKey(material);
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, result)
            .define(key, material.ingredient())
            .define('/', Items.STICK)
            .unlockedBy(getHasName(result), has(result));
        shape.rows().forEach(row -> builder.pattern(row.replace('$', key)));
        if (shape.type() == CLUB) {
          builder.group(material.id() + "_club");
        }

        Condition condition = shape.type() == DAGGER
            ? material.condition().withFarmersDelight(false)
            : material.condition();
        ResourceKey<Recipe<?>> recipe = recipeKey(material.path() + material.id() + "_" + shape.id());
        builder.save(conditioned(output, recipe, RecipeCategory.COMBAT, condition), recipe);
      }

      Item dagger = weapon(material.id(), DAGGER);
      ResourceKey<Recipe<?>> farmersDelightRecipe =
          recipeKey("compat/" + material.id() + "_dagger_farmersdelight");
      ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, dagger)
          .define('#', material.ingredient())
          .define('/', Items.STICK)
          .pattern(" #")
          .pattern("/ ")
          .unlockedBy(getHasName(dagger), has(dagger))
          .save(conditioned(output, farmersDelightRecipe, RecipeCategory.COMBAT,
              material.condition().withFarmersDelight(true)), farmersDelightRecipe);
    }

    private void createNetheriteSmithingRecipes() {
      for (BasicWeaponType type : RECIPE_TYPES) {
        Item diamond = weapon("diamond", type);
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(diamond),
                Ingredient.of(items.getOrThrow(ItemTags.NETHERITE_TOOL_MATERIALS)),
                RecipeCategory.COMBAT,
                weapon("netherite", type))
            .unlocks(getHasName(diamond), has(diamond))
            .save(output, recipeKey("netherite_" + type.getId() + "_smithing"));
      }
    }

    private void createRecyclingRecipes() {
      for (Recycling recycling : RECYCLING) {
        Item[] weapons = weapons(recycling.material());
        Item result = BuiltInRegistries.ITEM.getValue(Identifier.parse(recycling.result()));
        String criterion = "has_" + recycling.material() + "_weapon";
        for (String method : List.of("smelting", "blasting")) {
          SimpleCookingRecipeBuilder builder = method.equals("smelting")
              ? SimpleCookingRecipeBuilder.smelting(Ingredient.of(weapons), RecipeCategory.MISC,
              CookingBookCategory.MISC, result, 0.1F, 200)
              : SimpleCookingRecipeBuilder.blasting(Ingredient.of(weapons), RecipeCategory.MISC,
              CookingBookCategory.MISC, result, 0.1F, 100);
          ResourceKey<Recipe<?>> key = recyclingKey(recycling.recipeName(), method);
          builder.unlockedBy(criterion, hasAny(weapons))
              .save(conditioned(output, key, RecipeCategory.MISC, recycling.condition()), key);
        }
      }
    }

    private net.minecraft.advancements.Criterion<?> hasAny(Item[] weapons) {
      return inventoryTrigger(ItemPredicate.Builder.item().of(items, weapons));
    }

    private Item[] weapons(String material) {
      return RECIPE_TYPES.stream().map(type -> weapon(material, type)).toArray(Item[]::new);
    }

    private Item weapon(String material, BasicWeaponType type) {
      Identifier id = ID(material + "_" + type.getId());
      if (!BuiltInRegistries.ITEM.containsKey(id)) {
        throw new IllegalStateException("Missing registered item required for recipe datagen: " + id);
      }
      return BuiltInRegistries.ITEM.getValue(id);
    }
  }

  private record RecipeMaterial(MaterialEntry entry, Ingredient ingredient) {
    private String id() {
      return entry.prefix();
    }

    private boolean compat() {
      return WeaponRegistry.COMPAT_MATERIALS.contains(entry);
    }

    private String path() {
      return compat() ? "compat/" : "";
    }

    private Condition condition() {
      return compat() ? Condition.BRONZE : Condition.NONE;
    }

    private char longWeaponKey() {
      return switch (id()) {
        case "copper", "iron", "golden", "diamond" -> 'O';
        default -> '#';
      };
    }
  }

  private record Shape(BasicWeaponType type, String suffix, String key, List<String> rows) {
    private Shape(BasicWeaponType type, String suffix, String key, String... rows) {
      this(type, suffix, key, List.of(rows));
    }

    private String id() {
      return type.getId() + suffix;
    }

    private char materialKey(RecipeMaterial material) {
      return key.isEmpty() ? material.longWeaponKey() : key.charAt(0);
    }
  }

  private record Recycling(String material, String recipeName, String result,
                           Condition condition) {
  }

  private record ConditionalRecipe(RecipeCategory category, Condition condition) {
  }

  private record Condition(boolean bronze, @Nullable Boolean farmersDelight) {
    private static final Condition NONE = new Condition(false, null);
    private static final Condition BRONZE = new Condition(true, null);

    private Condition withFarmersDelight(boolean loaded) {
      return new Condition(bronze, loaded);
    }

    private ResourceCondition[] fabric() {
      List<ResourceCondition> conditions = new ArrayList<>();
      if (bronze) conditions.add(ResourceConditions.allModsLoaded("bronze"));
      if (farmersDelight != null) {
        ResourceCondition loaded = ResourceConditions.allModsLoaded("farmersdelight");
        conditions.add(farmersDelight ? loaded : ResourceConditions.not(loaded));
      }
      return conditions.toArray(ResourceCondition[]::new);
    }

    private JsonArray neoForge() {
      JsonArray conditions = new JsonArray();
      if (bronze) conditions.add(neoForgeLoaded("bronze"));
      if (farmersDelight != null) {
        JsonObject loaded = neoForgeLoaded("farmersdelight");
        conditions.add(farmersDelight ? loaded : neoForgeNot(loaded));
      }
      return conditions;
    }

    private static JsonObject neoForgeLoaded(String modId) {
      JsonObject condition = new JsonObject();
      condition.addProperty("type", "neoforge:mod_loaded");
      condition.addProperty("modid", modId);
      return condition;
    }

    private static JsonObject neoForgeNot(JsonObject value) {
      JsonObject condition = new JsonObject();
      condition.addProperty("type", "neoforge:not");
      condition.add("value", value);
      return condition;
    }
  }

  private RecipeOutput conditioned(RecipeOutput output, ResourceKey<Recipe<?>> key,
                                   RecipeCategory category, Condition condition) {
    if (condition.equals(Condition.NONE)) return output;
    conditionalRecipes.put(key.identifier(), new ConditionalRecipe(category, condition));
    return withConditions(output, condition.fabric());
  }

  private CompletableFuture<?> appendNeoForgeConditions(CachedOutput cache) {
    var recipePaths = output.createRegistryElementsPathProvider(Registries.RECIPE);
    var advancementPaths = output.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
    try {
      for (var entry : conditionalRecipes.entrySet()) {
        Identifier id = entry.getKey();
        ConditionalRecipe recipe = entry.getValue();
        appendCondition(cache, recipePaths.json(id), recipe.condition());
        Identifier advancementId = Identifier.fromNamespaceAndPath(id.getNamespace(),
            "recipes/" + recipe.category().getFolderName() + "/" + id.getPath());
        appendCondition(cache, advancementPaths.json(advancementId), recipe.condition());
      }
      return CompletableFuture.completedFuture(null);
    } catch (IOException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  private void appendCondition(CachedOutput cache, Path path, Condition condition) throws IOException {
    JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    var fabricConditions = json.remove("fabric:load_conditions");
    if (fabricConditions != null) {
      json.add("fabric:load_conditions", fabricConditions);
    }
    json.add("neoforge:conditions", condition.neoForge());
    byte[] contents = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
    // write directly through cache so that saveStable doesn't sort JSON keys
    cache.writeIfNeeded(path, contents, Hashing.sha256().hashBytes(contents));
  }

  private static ResourceKey<Recipe<?>> recipeKey(String path) {
    return ResourceKey.create(Registries.RECIPE, ID(path));
  }

  private static ResourceKey<Recipe<?>> recyclingKey(String material, String method) {
    return recipeKey("weapons_to_nuggets/" + material + "_nugget_from_" + method);
  }
}
