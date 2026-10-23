package tech.aaronf86.ametrine.damage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import tech.aaronf86.ametrine.damage.api.AmetrineAddon;
import tech.aaronf86.ametrine.damage.core.data.Profiles;

public class AmetrineDamageFabric implements ModInitializer {

  @Override
  public void onInitialize() {

    AmetrineDamage.init();
    FabricLoader.getInstance()
        .getEntrypoints(Constants.MOD_ID, AmetrineAddon.class)
        .forEach(AmetrineAddon::register);
    AmetrineDamage.registerAttributes(
        (id, attribute) -> Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, id, attribute));
    Profiles.reloadListeners()
        .forEach(ResourceLoader.get(PackType.SERVER_DATA)::registerReloadListener);
  }
}
