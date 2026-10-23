package tech.aaronf86.ametrine.damage.gametest;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import tech.aaronf86.ametrine.damage.api.AmetrineAddon;

public class AmetrineGameTestsFabric implements ModInitializer, AmetrineAddon {

  @Override
  public void onInitialize() {

    AmetrineGameTests.registerAll(
        (id, test) -> Registry.register(BuiltInRegistries.TEST_FUNCTION, id, test));
    AmetrineGameTests.registerHooks();
  }

  @Override
  public void register() {

    TestTypes.bootstrap();
  }
}
