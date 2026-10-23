package tech.aaronf86.ametrine.damage.api.defense;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import tech.aaronf86.ametrine.damage.api.AmetrineRegistries;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

/**
 * Fixed defensive stats from a single source, such as an armor piece or an entity's base profile.
 * Sources are combined into a {@link DefenseProfile}: defense and resistance add up, and negation
 * stacks multiplicatively. JSON form:
 *
 * <pre>{@code
 * { "defense": { "mymod:slash": 4 },
 *   "negation": { "mymod:fire": 0.1 },
 *   "resistance": { "mymod:bleed": 20, "mymod:poise": 5 } }
 * }</pre>
 *
 * @param defense flat defense per damage type
 * @param negation negation fraction per damage type, from -1 (weakness) to 1 (immune)
 * @param resistance extra resistance per status
 */
public record DefenseValues(
    Map<AmetrineDamageType, Float> defense,
    Map<AmetrineDamageType, Float> negation,
    Map<StatusType, Float> resistance) {

  /** No defensive stats. */
  public static final DefenseValues EMPTY = new DefenseValues(Map.of(), Map.of(), Map.of());

  public static final MapCodec<DefenseValues> MAP_CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      Codec.unboundedMap(AmetrineRegistries.DAMAGE_TYPES.codec(), Codec.FLOAT)
                          .optionalFieldOf("defense", Map.of())
                          .forGetter(DefenseValues::defense),
                      Codec.unboundedMap(
                              AmetrineRegistries.DAMAGE_TYPES.codec(), Codec.floatRange(-1, 1))
                          .optionalFieldOf("negation", Map.of())
                          .forGetter(DefenseValues::negation),
                      Codec.unboundedMap(AmetrineRegistries.STATUS_TYPES.codec(), Codec.FLOAT)
                          .optionalFieldOf("resistance", Map.of())
                          .forGetter(DefenseValues::resistance))
                  .apply(instance, DefenseValues::new));

  public static final Codec<DefenseValues> CODEC = MAP_CODEC.codec();

  public DefenseValues {

    defense = Map.copyOf(defense);
    negation = Map.copyOf(negation);
    resistance = Map.copyOf(resistance);
  }

  /** Flat defense against a type, or 0. */
  public float defense(AmetrineDamageType type) {

    return defense.getOrDefault(type, 0F);
  }

  /** Negation against a type, or 0. */
  public float negation(AmetrineDamageType type) {

    return negation.getOrDefault(type, 0F);
  }

  /** Extra resistance to a status, or 0. */
  public float resistance(StatusType status) {

    return resistance.getOrDefault(status, 0F);
  }
}
