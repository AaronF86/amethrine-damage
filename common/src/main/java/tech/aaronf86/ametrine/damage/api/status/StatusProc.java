package tech.aaronf86.ametrine.damage.api.status;

/**
 * What happens when a status meter fills up, e.g. bleed's burst of damage or poise's stagger. It
 * runs after the hit that filled the meter has finished, so the target's hurt cooldown is already
 * active: damage-dealing procs must allow for that and should mark their damage with {@code
 * PacketFlags.STATUS_PROC}.
 */
@FunctionalInterface
public interface StatusProc {

  /** Does nothing. */
  StatusProc NONE = context -> {};

  void trigger(StatusProcContext context);
}
