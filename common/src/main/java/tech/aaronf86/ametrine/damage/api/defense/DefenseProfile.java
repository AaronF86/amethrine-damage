package tech.aaronf86.ametrine.damage.api.defense;

import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

/**
 * Read-only view of an entity's defensive stats. Obtain one with {@code
 * AmetrineDamageApi.defenseOf}.
 */
public interface DefenseProfile {

  /** No defense, no negation and default resistances. */
  DefenseProfile NONE =
      new DefenseProfile() {

        @Override
        public float defense(AmetrineDamageType type) {
          return 0;
        }

        @Override
        public float negation(AmetrineDamageType type) {
          return 0;
        }

        @Override
        public float resistance(StatusType status) {
          return status.defaultResistance();
        }
      };

  /** Flat defense against a damage type. */
  float defense(AmetrineDamageType type);

  /** Fraction of a damage type that is negated, from -1 (takes double) to 1 (immune). */
  float negation(AmetrineDamageType type);

  /** How much buildup it takes to trigger a status, e.g. an entity's poise for a poise status. */
  float resistance(StatusType status);
}
