package tech.aaronf86.ametrine.damage.api.status;

import java.util.*;
import net.minecraft.resources.Identifier;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostic;
import tech.aaronf86.ametrine.damage.core.diagnostic.DiagnosticCode;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostics;

/**
 * Proc actions and the surrounding hooks. Ametrine holds every status meter; the action that runs
 * when a meter fills belongs to whichever mod owns the status. A mod that registers its own status
 * passes the action to {@link StatusType} directly. A mod that supplies the action for a status
 * registered elsewhere, such as a melee mod giving a poise status its stagger, calls {@link #bind}.
 */
public final class StatusProcs {

  /** Priority of hooks registered without one. Lower priorities run first. */
  public static final int DEFAULT_PRIORITY = 1000;

  private static final Map<StatusType, StatusProc> BOUND = new IdentityHashMap<>();
  private static final List<Hook> HOOKS = new ArrayList<>();

  private StatusProcs() {}

  /**
   * Supplies the action for a status that has none.
   *
   * @throws IllegalStateException if the status already has an action, from its registration or an
   *     earlier bind
   */
  public static void bind(StatusType status, StatusProc action) {

    Objects.requireNonNull(action);
    boolean fromRegistration = status.proc() != StatusProc.NONE;
    if (fromRegistration || BOUND.putIfAbsent(status, action) != null) {
      throw Diagnostics.failure(
          Diagnostic.of(
                  DiagnosticCode.PROC_ACTION_TAKEN,
                  "Status " + status.id() + " already has a proc action")
              .with("status owner", status.id().getNamespace())
              .with(
                  "action from",
                  fromRegistration
                      ? "the status's registration"
                      : "an earlier StatusProcs.bind call")
              .help(
                  "A status has one action. To run extra behaviour when it procs, register a StatusProcHook "
                      + "instead; its beforeProc can also cancel the action."));
    }
  }

  /**
   * The action that runs when a status procs: the bound one, else the one it was registered with.
   */
  public static StatusProc actionOf(StatusType status) {

    return BOUND.getOrDefault(status, status.proc());
  }

  /** Registers a hook at {@link #DEFAULT_PRIORITY}. */
  public static void registerHook(Identifier id, StatusProcHook hook) {

    registerHook(id, DEFAULT_PRIORITY, hook);
  }

  /**
   * Registers a hook. Hooks run by ascending priority, and hooks with equal priority run in
   * registration order.
   *
   * @throws IllegalArgumentException if the id is already used
   */
  public static void registerHook(Identifier id, int priority, StatusProcHook hook) {

    if (HOOKS.stream().anyMatch(entry -> entry.id().equals(id))) {
      throw Diagnostics.badArgument(
          Diagnostic.of(
                  DiagnosticCode.DUPLICATE_PROC_HOOK,
                  "Status proc hook " + id + " is already registered")
              .with("mod", id.getNamespace())
              .help(
                  "Pick a new id, or call StatusProcs.removeHook first to replace the existing hook."));
    }
    HOOKS.add(new Hook(id, priority, Objects.requireNonNull(hook)));
    HOOKS.sort(Comparator.comparingInt(Hook::priority));
  }

  /** Removes a hook. Returns false if not found. */
  public static boolean removeHook(Identifier id) {

    return HOOKS.removeIf(entry -> entry.id().equals(id));
  }

  /**
   * Runs a proc: every hook's {@link StatusProcHook#beforeProc}, then the action, then every {@link
   * StatusProcHook#afterProc}. A hook or action that throws is logged and skipped. Called by
   * Ametrine.
   */
  public static void fire(StatusProcContext context) {

    for (Hook entry : HOOKS) {
      try {
        if (!entry.hook().beforeProc(context)) {
          return;
        }
      } catch (RuntimeException e) {
        reportHookFailure(entry.id(), "beforeProc", context, e);
      }
    }
    try {
      actionOf(context.status()).trigger(context);
    } catch (RuntimeException e) {
      Identifier status = context.status().id();
      Diagnostics.reportRepeating(
          DiagnosticCode.PROC_ACTION_FAILED.id() + " " + status,
          () ->
              Diagnostic.of(
                      DiagnosticCode.PROC_ACTION_FAILED,
                      "Proc action for status " + status + " threw an exception")
                  .with(
                      "action from",
                      BOUND.containsKey(context.status())
                          ? "StatusProcs.bind"
                          : "the status's registration")
                  .help(
                      "This is a bug in the mod that supplies the action. Report it to that mod with this log. "
                          + "The meter has already reset, and afterProc hooks still run.")
                  .cause(e));
    }
    for (Hook entry : HOOKS) {
      try {
        entry.hook().afterProc(context);
      } catch (RuntimeException e) {
        reportHookFailure(entry.id(), "afterProc", context, e);
      }
    }
  }

  private static void reportHookFailure(
      Identifier hook, String method, StatusProcContext context, RuntimeException e) {

    Diagnostics.reportRepeating(
        DiagnosticCode.PROC_HOOK_FAILED.id() + " " + hook + " " + method,
        () ->
            Diagnostic.of(
                    DiagnosticCode.PROC_HOOK_FAILED,
                    "Status proc hook " + hook + " threw in " + method)
                .with("mod", hook.getNamespace())
                .with("status", context.status().id())
                .help(
                    "This is a bug in "
                        + hook.getNamespace()
                        + ". Report it to that mod with this log. The hook was "
                        + "skipped and the proc carried on.")
                .cause(e));
  }

  private record Hook(Identifier id, int priority, StatusProcHook hook) {}
}
