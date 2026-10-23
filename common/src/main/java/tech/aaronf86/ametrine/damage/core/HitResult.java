package tech.aaronf86.ametrine.damage.core;

/** What Ametrine decided for one hit, in place of vanilla's armor reduction. */
public sealed interface HitResult {

  /** The shared {@link Vanilla} instance. */
  HitResult VANILLA = new Vanilla();

  /** The hit has no typed damage: vanilla armor applies as usual, durability loss included. */
  record Vanilla() implements HitResult {}

  /** Ametrine handled the hit, and the amount replaces the damage after vanilla armor. */
  record Replaced(float amount) implements HitResult {}
}
