package tech.aaronf86.ametrine.damage.api.pipeline;

/** The stages a {@link DamagePacket} goes through, in order. */
public enum DamageStage {
  /** Turn the vanilla damage source and weapon into damage and buildup. */
  BUILD,
  /** Attacker-side changes: bonuses, crits, motion values. */
  MODIFY,
  /** Target-side reduction: motion value, defense formula, negation. */
  MITIGATE,
  /** Deal the final damage and add buildup. */
  APPLY,
  /** React to the finished hit: status procs, stagger, client sync. */
  POST
}
