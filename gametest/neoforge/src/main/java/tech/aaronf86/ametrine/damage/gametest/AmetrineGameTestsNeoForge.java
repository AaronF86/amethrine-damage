package tech.aaronf86.ametrine.damage.gametest;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

@SuppressWarnings("java:S1118")
@Mod(AmetrineGameTests.NAMESPACE)
public class AmetrineGameTestsNeoForge {

  public AmetrineGameTestsNeoForge(IEventBus eventBus) {

    TestTypes.bootstrap();
    AmetrineGameTests.registerHooks();
    eventBus.addListener(AmetrineGameTestsNeoForge::onRegister);
  }

  private static void onRegister(RegisterEvent event) {

    if (event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
      AmetrineGameTests.registerAll(
          (id, test) -> event.register(Registries.TEST_FUNCTION, id, () -> test));
    }
  }
}
