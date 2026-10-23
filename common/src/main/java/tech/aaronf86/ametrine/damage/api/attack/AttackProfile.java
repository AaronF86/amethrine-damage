package tech.aaronf86.ametrine.damage.api.attack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import tech.aaronf86.ametrine.damage.api.AmetrineRegistries;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.pipeline.DamagePacket;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

/**
 * What an attack deals. Damage is a split of the vanilla damage amount, so vanilla modifiers such
 * as Sharpness, Strength, crits and attack cooldown keep working: {@code {"mymod:strike": 1.0}}
 * turns all of it into strike damage, {@code {"mymod:slash": 0.7, "mymod:pierce": 0.3}} divides it,
 * and splits may add up to more or less than 1. Buildup is a flat amount per status, poise damage
 * included. JSON form:
 *
 * <pre>{@code
 * { "split": { "mymod:slash": 0.8, "mymod:fire": 0.4 },
 *   "buildup": { "mymod:bleed": 40, "mymod:poise": 10 } }
 * }</pre>
 *
 * @param split fraction of the vanilla amount dealt as each type
 * @param buildup flat buildup per status
 */
public record AttackProfile(Map<AmetrineDamageType, Float> split, Map<StatusType, Float> buildup) {

  /** Deals nothing. */
  public static final AttackProfile EMPTY = new AttackProfile(Map.of(), Map.of());

  public static final Codec<AttackProfile> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.unboundedMap(AmetrineRegistries.DAMAGE_TYPES.codec(), Codec.FLOAT)
                          .optionalFieldOf("split", Map.of())
                          .forGetter(AttackProfile::split),
                      Codec.unboundedMap(AmetrineRegistries.STATUS_TYPES.codec(), Codec.FLOAT)
                          .optionalFieldOf("buildup", Map.of())
                          .forGetter(AttackProfile::buildup))
                  .apply(instance, AttackProfile::new));

  public AttackProfile {

    split = Map.copyOf(split);
    buildup = Map.copyOf(buildup);
  }

  /**
   * Adds this profile's damage (as a split of the packet's vanilla amount) and buildup to a packet.
   */
  public void applyTo(DamagePacket packet) {

    split.forEach((type, fraction) -> packet.addDamage(type, packet.vanillaAmount() * fraction));
    buildup.forEach(packet::addBuildup);
  }
}
