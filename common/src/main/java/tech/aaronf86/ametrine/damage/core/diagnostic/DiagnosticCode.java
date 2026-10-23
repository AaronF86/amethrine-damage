package tech.aaronf86.ametrine.damage.core.diagnostic;

import org.slf4j.event.Level;

/**
 * Every problem Ametrine reports, with a searchable code. Each one is explained in docs/errors.md.
 */
public enum DiagnosticCode {

  // Data packs
  UNKNOWN_TARGET("AD1001", Level.WARN),
  DUPLICATE_TARGET("AD1002", Level.WARN),
  ABSENT_MOD_TARGET("AD1003", Level.INFO),

  // Runtime failures in other mods' code
  LISTENER_FAILED("AD2001", Level.ERROR),
  PROC_HOOK_FAILED("AD2002", Level.ERROR),
  PROC_ACTION_FAILED("AD2003", Level.ERROR),

  // Registration mistakes, thrown during startup
  REGISTRY_FROZEN("AD3001", Level.ERROR),
  DUPLICATE_ID("AD3002", Level.ERROR),
  DUPLICATE_LISTENER("AD3003", Level.ERROR),
  DUPLICATE_PROC_HOOK("AD3004", Level.ERROR),
  PROC_ACTION_TAKEN("AD3005", Level.ERROR),
  ATTRIBUTES_REGISTERED_TWICE("AD3006", Level.ERROR);

  private final String id;
  private final Level level;

  DiagnosticCode(String id, Level level) {

    this.id = id;
    this.level = level;
  }

  /** The searchable code, e.g. {@code AD1001}. */
  public String id() {

    return id;
  }

  /** The level the problem is logged at. */
  public Level level() {

    return level;
  }
}
