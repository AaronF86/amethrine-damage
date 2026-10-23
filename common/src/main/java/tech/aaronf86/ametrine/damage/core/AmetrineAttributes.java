package tech.aaronf86.ametrine.damage.core;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import tech.aaronf86.ametrine.damage.api.AmetrineRegistries;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.status.StatusType;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostic;
import tech.aaronf86.ametrine.damage.core.diagnostic.DiagnosticCode;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostics;
import tech.aaronf86.ametrine.damage.mixin.AttributeSupplierAccessor;

public final class AmetrineAttributes {

  private static final double MAX_VALUE = 1024;

  private static final Map<AmetrineDamageType, Holder<Attribute>> DEFENSE = new IdentityHashMap<>();
  private static final Map<AmetrineDamageType, Holder<Attribute>> NEGATION =
      new IdentityHashMap<>();
  private static final Map<StatusType, Holder<Attribute>> RESISTANCE = new IdentityHashMap<>();
  private static final Map<AttributeSupplier, AttributeSupplier> EXTENDED =
      new ConcurrentHashMap<>();
  private static boolean registered;

  private AmetrineAttributes() {}

  public static void registerAll(BiFunction<Identifier, Attribute, Holder<Attribute>> registrar) {

    if (registered) {
      throw Diagnostics.failure(
          Diagnostic.of(
                  DiagnosticCode.ATTRIBUTES_REGISTERED_TWICE,
                  "Ametrine's attributes were registered twice")
              .help(
                  "This is a bug in Ametrine Damage's loader entry point, not in another mod. Please report it."));
    }
    AmetrineRegistries.DAMAGE_TYPES.freeze();
    AmetrineRegistries.STATUS_TYPES.freeze();

    for (AmetrineDamageType type : AmetrineRegistries.DAMAGE_TYPES.values()) {
      DEFENSE.put(type, register(registrar, type.id(), "defense", 0, 0, MAX_VALUE));
      NEGATION.put(type, register(registrar, type.id(), "negation", 0, -1, 1));
    }
    for (StatusType status : AmetrineRegistries.STATUS_TYPES.values()) {
      RESISTANCE.put(
          status,
          register(registrar, status.id(), "resistance", status.defaultResistance(), 0, MAX_VALUE));
    }
    registered = true;
  }

  private static Holder<Attribute> register(
      BiFunction<Identifier, Attribute, Holder<Attribute>> registrar,
      Identifier owner,
      String kind,
      double base,
      double min,
      double max) {

    Identifier id =
        Identifier.fromNamespaceAndPath(owner.getNamespace(), kind + "." + owner.getPath());
    return registrar.apply(
        id,
        new RangedAttribute(
            "attribute.name." + id.getNamespace() + "." + id.getPath(), base, min, max));
  }

  public static Optional<Holder<Attribute>> defense(AmetrineDamageType type) {

    return Optional.ofNullable(DEFENSE.get(type));
  }

  public static Optional<Holder<Attribute>> negation(AmetrineDamageType type) {

    return Optional.ofNullable(NEGATION.get(type));
  }

  public static Optional<Holder<Attribute>> resistance(StatusType status) {

    return Optional.ofNullable(RESISTANCE.get(status));
  }

  public static AttributeSupplier extend(AttributeSupplier original) {

    if (!registered) {
      return original;
    }
    return EXTENDED.computeIfAbsent(original, AmetrineAttributes::copyWithOurs);
  }

  private static AttributeSupplier copyWithOurs(AttributeSupplier original) {

    AttributeSupplier.Builder builder = AttributeSupplier.builder();
    ((AttributeSupplierAccessor) original)
        .ametrine$getInstances()
        .forEach((attribute, instance) -> builder.add(attribute, instance.getBaseValue()));
    DEFENSE.values().forEach(attribute -> addIfMissing(builder, original, attribute));
    NEGATION.values().forEach(attribute -> addIfMissing(builder, original, attribute));
    RESISTANCE.values().forEach(attribute -> addIfMissing(builder, original, attribute));
    return builder.build();
  }

  private static void addIfMissing(
      AttributeSupplier.Builder builder, AttributeSupplier original, Holder<Attribute> attribute) {

    if (!original.hasAttribute(attribute)) {
      builder.add(attribute);
    }
  }
}
