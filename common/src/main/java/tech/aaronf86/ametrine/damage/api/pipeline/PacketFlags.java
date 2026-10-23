package tech.aaronf86.ametrine.damage.api.pipeline;

import tech.aaronf86.ametrine.damage.api.AmetrineDamageApi;

/** Built-in packet flags. */
public final class PacketFlags {

  /** The hit was a critical, e.g. a backstab or riposte. */
  public static final PacketFlag CRITICAL = create("critical");

  /** Skip defense and negation entirely, e.g. for falling or the void. */
  public static final PacketFlag BYPASS_DEFENSE = create("bypass_defense");

  /** The damage came from a status proc, so it must not add buildup. */
  public static final PacketFlag STATUS_PROC = create("status_proc");

  /**
   * The packet was built from a {@link DamageSpec}, so Ametrine's build step must not add profile
   * damage to it.
   */
  public static final PacketFlag EXPLICIT = create("explicit");

  private PacketFlags() {}

  private static PacketFlag create(String name) {

    return new PacketFlag(AmetrineDamageApi.id(name));
  }
}
