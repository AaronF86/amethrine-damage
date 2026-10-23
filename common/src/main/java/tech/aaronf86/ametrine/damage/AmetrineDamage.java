package tech.aaronf86.ametrine.damage;

import java.util.function.BiFunction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import tech.aaronf86.ametrine.damage.api.AmetrineDamageApi;
import tech.aaronf86.ametrine.damage.core.AmetrineAttributes;
import tech.aaronf86.ametrine.damage.core.CoreDefenseProfile;
import tech.aaronf86.ametrine.damage.core.DefaultListeners;

// Sharded Main Class
public final class AmetrineDamage {

  private AmetrineDamage() {}

  public static void init() {

    DefaultListeners.register();
    AmetrineDamageApi.setDefenseProvider(CoreDefenseProfile::new);
  }

  public static void registerAttributes(
      BiFunction<Identifier, Attribute, Holder<Attribute>> registrar) {

    AmetrineAttributes.registerAll(registrar);
    Constants.LOG.info("{} initialized", Constants.MOD_NAME);
  }
}
