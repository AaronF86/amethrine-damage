package tech.aaronf86.ametrine.damage.api.pipeline;

import java.util.*;
import net.minecraft.resources.Identifier;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostic;
import tech.aaronf86.ametrine.damage.core.diagnostic.DiagnosticCode;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostics;

/**
 * The ordered list of listeners every hit runs through. Ametrine's own behaviour is registered here
 * too, under ids in its namespace, so addons can run before or after it, replace it, or remove it.
 */
public final class DamagePipeline {

  /** Priority of Ametrine's own listeners. Lower priorities run first. */
  public static final int DEFAULT_PRIORITY = 1000;

  private static final Map<DamageStage, List<Entry>> STAGES = new EnumMap<>(DamageStage.class);

  static {
    for (DamageStage stage : DamageStage.values()) {
      STAGES.put(stage, new ArrayList<>());
    }
  }

  private DamagePipeline() {}

  /** Registers a listener at {@link #DEFAULT_PRIORITY}. */
  public static void register(DamageStage stage, Identifier id, DamageListener listener) {

    register(stage, id, DEFAULT_PRIORITY, listener);
  }

  /**
   * Registers a listener. Listeners in the same stage run by ascending priority, and listeners with
   * equal priority run in registration order.
   *
   * @throws IllegalArgumentException if the id is already used in any stage
   */
  public static void register(
      DamageStage stage, Identifier id, int priority, DamageListener listener) {

    Optional<DamageStage> existing = stageOf(id);
    if (existing.isPresent()) {
      throw Diagnostics.badArgument(
          Diagnostic.of(
                  DiagnosticCode.DUPLICATE_LISTENER,
                  "Damage listener " + id + " is already registered")
              .with("mod", id.getNamespace())
              .with("registered in", existing.get())
              .help(
                  "Use DamagePipeline.replace to swap an existing listener's behaviour, or pick a new id."));
    }
    List<Entry> entries = STAGES.get(stage);
    entries.add(new Entry(id, priority, listener));
    entries.sort(Comparator.comparingInt(Entry::priority));
  }

  /**
   * Swaps the behaviour of a registered listener, keeping its stage and priority. Returns false if
   * not found.
   */
  public static boolean replace(Identifier id, DamageListener listener) {

    for (List<Entry> entries : STAGES.values()) {
      for (int i = 0; i < entries.size(); i++) {
        Entry entry = entries.get(i);
        if (entry.id().equals(id)) {
          entries.set(i, new Entry(id, entry.priority(), listener));
          return true;
        }
      }
    }
    return false;
  }

  /** Removes a registered listener. Returns false if not found. */
  public static boolean remove(Identifier id) {

    for (List<Entry> entries : STAGES.values()) {
      if (entries.removeIf(entry -> entry.id().equals(id))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Runs a packet through every stage, stopping early if it is cancelled. Called by Ametrine's
   * damage hook.
   *
   * @throws DamageListenerException if a listener throws, naming that listener
   */
  public static void process(DamagePacket packet) {

    for (DamageStage stage : DamageStage.values()) {
      for (Entry entry : STAGES.get(stage)) {
        if (packet.isCancelled()) {
          return;
        }
        try {
          entry.listener().handle(packet);
        } catch (RuntimeException e) {
          throw new DamageListenerException(stage, entry.id(), e);
        }
      }
    }
  }

  private static Optional<DamageStage> stageOf(Identifier id) {

    return STAGES.entrySet().stream()
        .filter(stage -> stage.getValue().stream().anyMatch(entry -> entry.id().equals(id)))
        .map(Map.Entry::getKey)
        .findFirst();
  }

  private record Entry(Identifier id, int priority, DamageListener listener) {}
}
