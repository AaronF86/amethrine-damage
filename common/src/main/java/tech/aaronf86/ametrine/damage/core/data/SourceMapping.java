package tech.aaronf86.ametrine.damage.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import tech.aaronf86.ametrine.damage.api.attack.AttackProfile;

public record SourceMapping(AttackProfile attack, boolean bypassDefense) {

  public static final MapCodec<SourceMapping> MAP_CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      AttackProfile.CODEC.fieldOf("attack").forGetter(SourceMapping::attack),
                      Codec.BOOL
                          .optionalFieldOf("bypass_defense", false)
                          .forGetter(SourceMapping::bypassDefense))
                  .apply(instance, SourceMapping::new));
}
