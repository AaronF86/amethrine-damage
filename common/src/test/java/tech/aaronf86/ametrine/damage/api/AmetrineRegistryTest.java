package tech.aaronf86.ametrine.damage.api;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.damage.DamageCategory;

class AmetrineRegistryTest {

  private static AmetrineRegistry<AmetrineDamageType> registry() {

    return new AmetrineRegistry<>(
        Identifier.fromNamespaceAndPath("test", "types"), AmetrineDamageType::id);
  }

  private static AmetrineDamageType type(String path) {

    return new AmetrineDamageType(
        Identifier.fromNamespaceAndPath("test", path), DamageCategory.PHYSICAL);
  }

  @Test
  void registersAndLooksUp() {

    var registry = registry();
    var slash = registry.register(type("slash"));
    assertSame(slash, registry.get(Identifier.fromNamespaceAndPath("test", "slash")).orElseThrow());
    assertTrue(registry.get(Identifier.fromNamespaceAndPath("test", "nope")).isEmpty());
    assertEquals(1, registry.values().size());
  }

  @Test
  void rejectsDuplicatesAndFrozen() {

    var registry = registry();
    registry.register(type("slash"));
    var duplicate = type("slash");
    assertThrows(IllegalArgumentException.class, () -> registry.register(duplicate));
    registry.freeze();
    var pierce = type("pierce");
    assertThrows(IllegalStateException.class, () -> registry.register(pierce));
  }

  @Test
  void codecRoundTrips() {

    var registry = registry();
    var slash = registry.register(type("slash"));
    var json = registry.codec().encodeStart(JsonOps.INSTANCE, slash).getOrThrow();
    assertEquals(new JsonPrimitive("test:slash"), json);
    assertSame(slash, registry.codec().parse(JsonOps.INSTANCE, json).getOrThrow());
    assertTrue(registry.codec().parse(JsonOps.INSTANCE, new JsonPrimitive("test:nope")).isError());
  }
}
