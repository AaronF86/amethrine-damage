package tech.aaronf86.ametrine.damage.gametest;

import tech.aaronf86.ametrine.damage.api.AmetrineRegistries;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.damage.DamageCategory;
import tech.aaronf86.ametrine.damage.api.status.StatusProc;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

public final class TestTypes {

  public static final AmetrineDamageType SLASH = register("slash", DamageCategory.PHYSICAL);
  public static final AmetrineDamageType FIRE = register("fire", DamageCategory.ELEMENTAL);
  public static final AmetrineDamageType MAGIC = register("magic", DamageCategory.ELEMENTAL);

  public static final StatusType BLEED = status("bleed", 100, 10);
  public static final StatusType POISE = status("poise", 0, 10);

  private TestTypes() {}

  @SuppressWarnings("EmptyMethod")
  public static void bootstrap() {
    // Intentionally empty
  }

  private static AmetrineDamageType register(String name, DamageCategory category) {

    return AmetrineRegistries.DAMAGE_TYPES.register(
        new AmetrineDamageType(AmetrineGameTests.id(name), category));
  }

  private static StatusType status(String name, float defaultResistance, float decayPerSecond) {

    return AmetrineRegistries.STATUS_TYPES.register(
        new StatusType(
            AmetrineGameTests.id(name), defaultResistance, decayPerSecond, StatusProc.NONE));
  }
}
