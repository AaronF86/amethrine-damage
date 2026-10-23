package tech.aaronf86.ametrine.damage.core.data;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import tech.aaronf86.ametrine.damage.api.AmetrineDamageApi;

public final class Profiles {

  @SuppressWarnings("java:S3077")
  private static volatile Map<Identifier, CombatProfile> items = Map.of();

  @SuppressWarnings("java:S3077")
  private static volatile Map<Identifier, CombatProfile> entities = Map.of();

  @SuppressWarnings("java:S3077")
  private static volatile Map<Identifier, SourceMapping> sources = Map.of();

  private Profiles() {}

  public static Optional<CombatProfile> item(ItemStack stack) {

    return stack.isEmpty()
        ? Optional.empty()
        : Optional.ofNullable(items.get(BuiltInRegistries.ITEM.getKey(stack.getItem())));
  }

  public static Optional<CombatProfile> entity(Entity entity) {

    return Optional.ofNullable(
        entities.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
  }

  public static Optional<SourceMapping> source(DamageSource source) {

    return source.typeHolder().unwrapKey().map(key -> sources.get(key.identifier()));
  }

  public static Map<Identifier, PreparableReloadListener> reloadListeners() {

    String root = AmetrineDamageApi.NAMESPACE + "/";
    return Map.of(
        AmetrineDamageApi.id("item_profiles"),
        new ProfileReloadListener<>(
            root + "item_profiles",
            "items",
            "item",
            Optional.of(BuiltInRegistries.ITEM),
            CombatProfile.MAP_CODEC,
            loaded -> items = loaded),
        AmetrineDamageApi.id("entity_profiles"),
        new ProfileReloadListener<>(
            root + "entity_profiles",
            "entities",
            "entity type",
            Optional.of(BuiltInRegistries.ENTITY_TYPE),
            CombatProfile.MAP_CODEC,
            loaded -> entities = loaded),
        // Damage types live in a data pack registry, which isn't readable from here, so they are
        // not checked
        AmetrineDamageApi.id("source_mappings"),
        new ProfileReloadListener<>(
            root + "source_mappings",
            "damage_types",
            "damage type",
            Optional.empty(),
            SourceMapping.MAP_CODEC,
            loaded -> sources = loaded));
  }
}
