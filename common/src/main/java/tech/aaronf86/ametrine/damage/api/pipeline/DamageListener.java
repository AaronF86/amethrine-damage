package tech.aaronf86.ametrine.damage.api.pipeline;

/** A step in the damage pipeline. */
@FunctionalInterface
public interface DamageListener {

  /** Reads or changes the packet. Call {@link DamagePacket#cancel()} to stop the hit. */
  void handle(DamagePacket packet);
}
