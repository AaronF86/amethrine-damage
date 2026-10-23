package tech.aaronf86.ametrine.damage.api.defense;

/**
 * Helpers for percentage damage negation. Negation is a fraction from -1 to 1: 0 is neutral, 1 is
 * immune, and negative values are weaknesses, e.g. -0.3 takes 30% extra damage.
 */
public final class Negation {

  private Negation() {}

  /** Stacks negation sources multiplicatively, so two 50% sources give 75%, never 100%. */
  public static float combine(float... negations) {

    float taken = 1;
    for (float negation : negations) {
      taken *= 1 - clamp(negation);
    }
    return 1 - taken;
  }

  /** Applies negation to an amount of damage. */
  public static float apply(float damage, float negation) {

    return damage * (1 - clamp(negation));
  }

  /** Clamps a negation value to [-1, 1]. */
  public static float clamp(float negation) {

    return Math.clamp(negation, -1F, 1F);
  }
}
