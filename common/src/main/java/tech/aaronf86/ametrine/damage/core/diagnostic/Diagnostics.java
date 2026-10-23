package tech.aaronf86.ametrine.damage.core.diagnostic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.spi.LoggingEventBuilder;
import tech.aaronf86.ametrine.damage.Constants;

/** Writes diagnostics to the log. */
public final class Diagnostics {

  private static final Map<String, AtomicLong> REPEATS = new ConcurrentHashMap<>();

  private Diagnostics() {}

  /** Logs a diagnostic at its code's level, with the stack trace of its cause if it has one. */
  public static void report(Diagnostic diagnostic) {

    LoggingEventBuilder event = Constants.LOG.atLevel(diagnostic.code().level());
    event = diagnostic.cause().map(event::setCause).orElse(event);
    event.log(diagnostic.render());
  }

  /**
   * Logs a problem that can happen on every hit or tick, such as a broken listener. The first time
   * a key is seen the full diagnostic is logged; after that only a one-line count at 10, 100,
   * 1000... repeats, so a broken mod cannot flood the log.
   */
  public static void reportRepeating(String key, Supplier<Diagnostic> diagnostic) {

    long count = REPEATS.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
    if (count == 1) {
      report(diagnostic.get());
    } else if (isPowerOfTen(count)) {
      Diagnostic repeated = diagnostic.get();
      Constants.LOG
          .atLevel(repeated.code().level())
          .log(
              "{} (happened {} times, only the first is logged in full)",
              repeated.headline(),
              count);
    }
  }

  /**
   * Builds the exception for a startup mistake. Its message is the rendered diagnostic, so crash
   * reports show it.
   */
  public static IllegalStateException failure(Diagnostic diagnostic) {

    return new IllegalStateException(diagnostic.render());
  }

  /** Like {@link #failure}, for a bad argument. */
  public static IllegalArgumentException badArgument(Diagnostic diagnostic) {

    return new IllegalArgumentException(diagnostic.render());
  }

  private static boolean isPowerOfTen(long count) {

    long power = 10;
    while (power < count) {
      power *= 10;
    }
    return power == count;
  }
}
