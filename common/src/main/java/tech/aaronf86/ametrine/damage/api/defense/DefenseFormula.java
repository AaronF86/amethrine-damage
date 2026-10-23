package tech.aaronf86.ametrine.damage.api.defense;

/** Reduces one damage type's attack value by the target's flat defense against that type. */
@FunctionalInterface
public interface DefenseFormula {

  /**
   * @param attack incoming damage of one type, before negation
   * @param defense the target's flat defense against that type
   * @return damage remaining after defense
   */
  float apply(float attack, float defense);
}
