package tech.aaronf86.ametrine.damage.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import tech.aaronf86.ametrine.damage.api.attack.AttackProfile;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.damage.DamageCategory;
import tech.aaronf86.ametrine.damage.api.pipeline.DamageSpec;
import tech.aaronf86.ametrine.damage.api.status.StatusProc;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

class DamageSpecTest {

  private static final float EPSILON = 1e-4F;
  private static final AmetrineDamageType SLASH = type("slash", DamageCategory.PHYSICAL);
  private static final AmetrineDamageType STRIKE = type("strike", DamageCategory.PHYSICAL);
  private static final AmetrineDamageType FIRE = type("fire", DamageCategory.ELEMENTAL);
  private static final StatusType BLEED =
      new StatusType(Identifier.fromNamespaceAndPath("test", "bleed"), 100, 10, StatusProc.NONE);
  private static final StatusType POISE =
      new StatusType(Identifier.fromNamespaceAndPath("test", "poise"), 0, 10, StatusProc.NONE);

  private static AmetrineDamageType type(String path, DamageCategory category) {

    return new AmetrineDamageType(Identifier.fromNamespaceAndPath("test", path), category);
  }

  @Test
  void sumsDamageOfTheSameType() {

    DamageSpec spec =
        DamageSpec.builder().damage(SLASH, 1.4F).damage(STRIKE, 0.6F).damage(SLASH, 1).build();
    assertEquals(2.4F, spec.damage().get(SLASH), EPSILON);
    assertEquals(3.0F, spec.totalDamage(), EPSILON);
  }

  @Test
  void profileSplitsTheBaseAmountAndKeepsBuildupFlat() {

    AttackProfile profile =
        new AttackProfile(Map.of(SLASH, 0.7F, STRIKE, 0.3F), Map.of(BLEED, 20F, POISE, 12F));
    DamageSpec spec = DamageSpec.builder().profile(profile, 2).build();
    assertEquals(1.4F, spec.damage().get(SLASH), EPSILON);
    assertEquals(0.6F, spec.damage().get(STRIKE), EPSILON);
    assertEquals(20F, spec.buildup().get(BLEED), EPSILON);
    assertEquals(12F, spec.buildup().get(POISE), EPSILON);
  }

  @Test
  void blockedHitCarriesOnlyBuildup() {

    DamageSpec spec = DamageSpec.builder().buildup(POISE, 50).build();
    assertEquals(0F, spec.totalDamage(), EPSILON);
    assertEquals(50F, spec.buildup().get(POISE), EPSILON);
  }

  @Test
  void rejectsNegativeAndNonFiniteAmounts() {

    DamageSpec.Builder builder = DamageSpec.builder();
    assertThrows(IllegalArgumentException.class, () -> builder.damage(FIRE, -1));
    assertThrows(IllegalArgumentException.class, () -> builder.damage(FIRE, Float.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> builder.buildup(POISE, Float.POSITIVE_INFINITY));
    assertThrows(IllegalArgumentException.class, () -> builder.motionValue(-0.5F));
  }
}
