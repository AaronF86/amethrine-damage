package tech.aaronf86.ametrine.damage.api.defense;

/** Built-in defense formulas. */
public final class DefenseFormulas {

  /**
   * The default curve. The attack/defense ratio picks a multiplier between 0.1 and 0.9: defense can
   * never cut damage below 10%, and big hits always lose at least 10%. Zero defense counts as no
   * defense at all, so the attack passes through unchanged.
   */
  public static final DefenseFormula DEFAULT_CURVE = DefenseFormulas::defaultCurve;

  /** Ignores defense entirely. */
  public static final DefenseFormula NONE = (attack, defense) -> attack;

  private DefenseFormulas() {}

  private static float defaultCurve(float attack, float defense) {

    if (attack <= 0) {
      return 0;
    }
    if (defense <= 0) {
      return attack;
    }
    float ratio = attack / defense;
    float multiplier;
    if (ratio > 8) {
      multiplier = 0.9F;
    } else if (ratio > 2.5F) {
      float t = (8 - ratio) / 5.5F;
      multiplier = 0.9F - 0.2F * t * t;
    } else if (ratio > 1) {
      float t = (2.5F - ratio) / 1.5F;
      multiplier = 0.7F - 0.3F * t * t;
    } else if (ratio > 0.125F) {
      float t = (ratio - 0.125F) / 0.875F;
      multiplier = 0.1F + 0.3F * t * t;
    } else {
      multiplier = 0.1F;
    }
    return attack * multiplier;
  }
}
