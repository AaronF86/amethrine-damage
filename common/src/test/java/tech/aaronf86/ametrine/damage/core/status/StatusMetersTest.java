package tech.aaronf86.ametrine.damage.core.status;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import tech.aaronf86.ametrine.damage.api.status.StatusProc;
import tech.aaronf86.ametrine.damage.api.status.StatusProcContext;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

class StatusMetersTest {

  private static final float EPSILON = 1e-4F;
  private static final StatusType POISE =
      new StatusType(Identifier.fromNamespaceAndPath("test", "poise"), 0, 10, StatusProc.NONE);

  @Test
  void fillsAtResistanceAndResets() {

    StatusMeters meters = new StatusMeters();
    assertFalse(meters.add(POISE, 50, 60));
    assertEquals(50, meters.get(POISE), EPSILON);
    assertTrue(meters.add(POISE, 25, 60));
    assertEquals(0, meters.get(POISE), EPSILON);
  }

  @Test
  void zeroResistanceProcsOnAnyBuildup() {

    assertTrue(new StatusMeters().add(POISE, 1, 0));
  }

  @Test
  void decaysAndEmpties() {

    StatusMeters meters = new StatusMeters();
    meters.add(POISE, 15, 60);
    meters.decay(1);
    assertEquals(5, meters.get(POISE), EPSILON);
    meters.decay(1);
    assertEquals(0, meters.get(POISE), EPSILON);
  }

  @Test
  void drainTakesQueuedProcsOnce() {

    StatusMeters meters = new StatusMeters();
    assertTrue(meters.drain().isEmpty());
    meters.queue(new StatusProcContext(null, null, POISE, null));
    assertEquals(1, meters.drain().size());
    assertTrue(meters.drain().isEmpty());
  }
}
