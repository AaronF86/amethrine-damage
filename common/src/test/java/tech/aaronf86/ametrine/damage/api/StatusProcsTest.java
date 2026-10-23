package tech.aaronf86.ametrine.damage.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import tech.aaronf86.ametrine.damage.api.status.*;

class StatusProcsTest {

  // Not registered: these tests only exercise binding and hooks
  private static StatusType status(String path, StatusProc proc) {

    return new StatusType(Identifier.fromNamespaceAndPath("test", path), 100, 10, proc);
  }

  // Hooks and actions here never touch the level, target or hit
  private static StatusProcContext context(StatusType status) {

    return new StatusProcContext(null, null, status, null);
  }

  @Test
  void bindsOnceAndOnlyWhenThereIsNoAction() {

    StatusType poise = status("bind_poise", StatusProc.NONE);
    StatusProc stagger = context -> {};
    StatusProcs.bind(poise, stagger);
    assertSame(stagger, StatusProcs.actionOf(poise));
    assertThrows(IllegalStateException.class, () -> StatusProcs.bind(poise, context -> {}));

    StatusType darkness = status("bind_darkness", context -> {});
    assertThrows(IllegalStateException.class, () -> StatusProcs.bind(darkness, context -> {}));
  }

  @Test
  void hooksRunAroundTheActionAndCanCancelIt() {

    List<String> calls = new ArrayList<>();
    boolean[] cancel = {false};
    StatusType darkness = status("hook_darkness", context -> calls.add("action"));
    Identifier late = Identifier.fromNamespaceAndPath("test", "late");
    Identifier early = Identifier.fromNamespaceAndPath("test", "early");
    StatusProcs.registerHook(
        late,
        2000,
        new StatusProcHook() {

          @Override
          public boolean beforeProc(StatusProcContext context) {
            calls.add("late before");
            return true;
          }

          @Override
          public void afterProc(StatusProcContext context) {
            calls.add("late after");
          }
        });
    StatusProcs.registerHook(
        early,
        500,
        new StatusProcHook() {

          @Override
          public boolean beforeProc(StatusProcContext context) {
            calls.add("early before");
            return !cancel[0];
          }
        });
    try {
      StatusProcs.fire(context(darkness));
      assertEquals(List.of("early before", "late before", "action", "late after"), calls);

      calls.clear();
      cancel[0] = true;
      StatusProcs.fire(context(darkness));
      assertEquals(List.of("early before"), calls);

      StatusProcHook duplicate = new StatusProcHook() {};
      assertThrows(
          IllegalArgumentException.class, () -> StatusProcs.registerHook(early, duplicate));
    } finally {
      StatusProcs.removeHook(late);
      StatusProcs.removeHook(early);
    }
  }

  @Test
  void failingActionDoesNotStopTheHooks() {

    List<String> calls = new ArrayList<>();
    StatusType broken =
        status(
            "broken",
            context -> {
              throw new IllegalStateException("boom");
            });
    Identifier id = Identifier.fromNamespaceAndPath("test", "after_broken");
    StatusProcs.registerHook(
        id,
        new StatusProcHook() {

          @Override
          public void afterProc(StatusProcContext context) {
            calls.add("after");
          }
        });
    try {
      StatusProcs.fire(context(broken));
      assertEquals(List.of("after"), calls);
    } finally {
      StatusProcs.removeHook(id);
    }
  }
}
