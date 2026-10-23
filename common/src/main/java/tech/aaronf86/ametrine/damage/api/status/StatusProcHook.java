package tech.aaronf86.ametrine.damage.api.status;

/**
 * Runs around every status proc, whichever mod owns the status. Register with {@link
 * StatusProcs#registerHook}.
 */
public interface StatusProcHook {

  /**
   * Runs before the proc's action. Return false to cancel the action, e.g. for a hyper armor buff
   * that prevents stagger. The meter has already reset either way.
   */
  default boolean beforeProc(StatusProcContext context) {

    return true;
  }

  /** Runs after the proc's action, only if no hook cancelled it. */
  default void afterProc(StatusProcContext context) {}
}
