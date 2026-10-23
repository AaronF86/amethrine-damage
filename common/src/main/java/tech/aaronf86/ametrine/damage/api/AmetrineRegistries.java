package tech.aaronf86.ametrine.damage.api;

import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

/** The registries addons add content to. */
public final class AmetrineRegistries {

  /** Damage types such as slash or fire. */
  public static final AmetrineRegistry<AmetrineDamageType> DAMAGE_TYPES =
      new AmetrineRegistry<>(AmetrineDamageApi.id("damage_type"), AmetrineDamageType::id);

  /** Status types that build up, such as bleed or frostbite. */
  public static final AmetrineRegistry<StatusType> STATUS_TYPES =
      new AmetrineRegistry<>(AmetrineDamageApi.id("status_type"), StatusType::id);

  private AmetrineRegistries() {}
}
