package tech.aaronf86.ametrine.damage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import tech.aaronf86.ametrine.damage.core.data.Profiles;

@SuppressWarnings("java:S1118")
@Mod(Constants.MOD_ID)
public class AmetrineDamageNeoForge {

  public AmetrineDamageNeoForge(IEventBus eventBus) {

    AmetrineDamage.init();
    eventBus.addListener(AmetrineDamageNeoForge::onRegister);
    NeoForge.EVENT_BUS.addListener(AmetrineDamageNeoForge::onAddReloadListeners);
  }

  private static void onRegister(RegisterEvent event) {

    if (event.getRegistryKey().equals(Registries.ATTRIBUTE)) {
      AmetrineDamage.registerAttributes(
          (id, attribute) -> {
            event.register(Registries.ATTRIBUTE, id, () -> attribute);
            return BuiltInRegistries.ATTRIBUTE.get(id).orElseThrow();
          });
    }
  }

  private static void onAddReloadListeners(AddServerReloadListenersEvent event) {

    Profiles.reloadListeners().forEach(event::addListener);
  }
}
