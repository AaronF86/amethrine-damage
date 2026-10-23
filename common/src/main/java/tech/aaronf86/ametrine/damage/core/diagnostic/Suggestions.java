package tech.aaronf86.ametrine.damage.core.diagnostic;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/** "Did you mean" suggestions for mistyped ids. */
public final class Suggestions {

  private Suggestions() {}

  /** The candidate closest to the input, if any is close enough to be a likely typo. */
  public static Optional<String> closest(String input, Collection<String> candidates) {

    int limit = Math.max(2, input.length() / 4);
    return candidates.stream()
        .filter(candidate -> Math.abs(candidate.length() - input.length()) <= limit)
        .map(candidate -> new Match(candidate, distance(input, candidate)))
        .filter(match -> match.distance() <= limit)
        .min(Comparator.comparingInt(Match::distance))
        .map(Match::candidate);
  }

  // Levenshtein distance, two rows at a time
  static int distance(String a, String b) {

    int[] previous = new int[b.length() + 1];
    int[] current = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      previous[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      current[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int substitute = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
        current[j] = Math.min(substitute, Math.min(previous[j], current[j - 1]) + 1);
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[b.length()];
  }

  private record Match(String candidate, int distance) {}
}
