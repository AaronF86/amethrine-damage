package tech.aaronf86.ametrine.damage.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tech.aaronf86.ametrine.damage.api.defense.DefenseFormula;
import tech.aaronf86.ametrine.damage.api.defense.DefenseFormulas;
import tech.aaronf86.ametrine.damage.api.defense.Negation;

class DefenseFormulasTest {

  private static final DefenseFormula CURVE = DefenseFormulas.DEFAULT_CURVE;
  private static final float EPSILON = 1e-4F;

  @Test
  void curveHitsKnownPoints() {

    assertEquals(1.0F, CURVE.apply(10, 100), EPSILON); // ratio 0.1   -> 10%
    assertEquals(0.125F, CURVE.apply(1.25F, 10), EPSILON); // ratio 0.125 -> 10%
    assertEquals(4.0F, CURVE.apply(10, 10), EPSILON); // ratio 1     -> 40%
    assertEquals(17.5F, CURVE.apply(25, 10), EPSILON); // ratio 2.5   -> 70%
    assertEquals(72.0F, CURVE.apply(80, 10), EPSILON); // ratio 8     -> 90%
    assertEquals(90.0F, CURVE.apply(100, 1), EPSILON); // ratio 100   -> 90%
  }

  @Test
  void curveIsMonotonic() {

    float previous = 0;
    for (float attack = 0.1F; attack < 200; attack += 0.1F) {
      float result = CURVE.apply(attack, 10);
      assertTrue(result >= previous, "decreased at attack " + attack);
      previous = result;
    }
  }

  @Test
  void zeroDefenseOrAttack() {

    assertEquals(7.0F, CURVE.apply(7, 0), EPSILON);
    assertEquals(0.0F, CURVE.apply(0, 10), EPSILON);
  }

  @Test
  void negationStacksMultiplicatively() {

    assertEquals(0.75F, Negation.combine(0.5F, 0.5F), EPSILON);
    assertEquals(0.0F, Negation.combine(), EPSILON);
    assertEquals(1.0F, Negation.combine(1.5F), EPSILON);
    assertEquals(6.0F, Negation.apply(10, 0.4F), EPSILON);
    // Weaknesses stack too: -30% and 50% gives 1.3 * 0.5 = 0.65 taken
    assertEquals(0.35F, Negation.combine(-0.3F, 0.5F), EPSILON);
    assertEquals(13.0F, Negation.apply(10, -0.3F), EPSILON);
    assertEquals(-1.0F, Negation.clamp(-5), EPSILON);
  }
}
