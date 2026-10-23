package tech.aaronf86.ametrine.damage.api;

/**
 * Entry point for mods that extend Ametrine Damage.
 *
 * <p>On Fabric, list your implementation under the {@code "ametrinedamage"} entrypoint in
 * fabric.mod.json. Ametrine calls it before its registries freeze, regardless of mod load order.
 *
 * <p>On NeoForge, call your registration code from your mod constructor. The registries freeze
 * later, during registry events.
 */
public interface AmetrineAddon {

  /** Register damage types, status types and pipeline listeners here. */
  void register();
}
