package tech.aaronf86.ametrine.damage.core.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import tech.aaronf86.ametrine.damage.api.attack.AttackProfile;
import tech.aaronf86.ametrine.damage.api.defense.DefenseValues;

public record CombatProfile(Optional<AttackProfile> attack, DefenseValues defense) {

  public static final MapCodec<CombatProfile> MAP_CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      AttackProfile.CODEC
                          .optionalFieldOf("attack")
                          .forGetter(CombatProfile::attack),
                      DefenseValues.CODEC
                          .optionalFieldOf("defense", DefenseValues.EMPTY)
                          .forGetter(CombatProfile::defense))
                  .apply(instance, CombatProfile::new));
}
