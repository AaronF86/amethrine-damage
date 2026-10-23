package tech.aaronf86.ametrine.damage.core.diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One problem, described for the person who has to fix it: what went wrong, the facts that locate
 * it, and what to do about it. The stack trace of an unexpected exception is kept as the cause, so
 * the log still has it.
 *
 * <pre>
 * [AD1001] Item profile targets an unknown item: minecraft:wooden_swrod
 *    | file: data/mypack/ametrinedamage/item_profiles/swords.json
 *    | pack: file/MyPack
 *    = help: Did you mean minecraft:wooden_sword?
 * </pre>
 */
public record Diagnostic(
    DiagnosticCode code,
    String message,
    List<Detail> details,
    Optional<String> help,
    Optional<Throwable> cause) {

  public Diagnostic {

    details = List.copyOf(details);
  }

  public static Diagnostic of(DiagnosticCode code, String message) {

    return new Diagnostic(code, message, List.of(), Optional.empty(), Optional.empty());
  }

  /** Adds a labelled fact, such as the file or the mod involved. */
  public Diagnostic with(String label, Object value) {

    List<Detail> more = new ArrayList<>(details);
    more.add(new Detail(label, String.valueOf(value)));
    return new Diagnostic(code, message, more, help, cause);
  }

  /** Sets what the reader should do to fix the problem. */
  public Diagnostic help(String text) {

    return new Diagnostic(code, message, details, Optional.of(text), cause);
  }

  /** Attaches the exception behind the problem, so its stack trace is logged. */
  public Diagnostic cause(Throwable throwable) {

    return new Diagnostic(code, message, details, help, Optional.of(throwable));
  }

  /** The first line on its own, e.g. for a one-line repeat notice. */
  public String headline() {

    return "[" + code.id() + "] " + message;
  }

  /** The full plain-text report, without the stack trace. Safe for log files: no colour codes. */
  public String render() {

    int width = details.stream().mapToInt(detail -> detail.label().length()).max().orElse(0);
    StringBuilder out = new StringBuilder(headline());
    for (Detail detail : details) {
      out.append("\n   | ")
          .append(detail.label())
          .append(':')
          .repeat(" ", width - detail.label().length() + 1)
          .append(detail.value());
    }
    help.ifPresent(text -> out.append("\n   = help: ").append(text));
    cause.ifPresent(throwable -> out.append("\n   = caused by: ").append(throwable));
    return out.toString();
  }

  public record Detail(String label, String value) {}
}
