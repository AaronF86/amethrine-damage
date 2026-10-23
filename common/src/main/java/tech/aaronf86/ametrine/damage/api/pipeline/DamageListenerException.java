package tech.aaronf86.ametrine.damage.api.pipeline;

import java.io.Serial;
import net.minecraft.resources.Identifier;

/**
 * A {@link DamageListener} threw while a hit ran through the {@link DamagePipeline}. The cause is
 * the original exception.
 */
public final class DamageListenerException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final DamageStage stage;
  // Identifier is not Serializable; the id is still in the message
  private final transient Identifier listener;

  public DamageListenerException(DamageStage stage, Identifier listener, RuntimeException cause) {

    super("Damage listener " + listener + " failed in stage " + stage, cause);
    this.stage = stage;
    this.listener = listener;
  }

  /** The stage the listener was registered in. */
  public DamageStage stage() {

    return stage;
  }

  /** The id the listener was registered under; its namespace is usually the mod that owns it. */
  public Identifier listener() {

    return listener;
  }
}
