package tech.aaronf86.ametrine.damage.api.status;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import tech.aaronf86.ametrine.damage.api.pipeline.DamagePacket;

/**
 * A status meter that filled up.
 *
 * @param level the target's level
 * @param target the entity whose meter filled
 * @param status the status that procced
 * @param hit the finished hit that filled the meter; its source, flags and context (e.g. attack
 *     direction) are readable
 */
public record StatusProcContext(
    ServerLevel level, LivingEntity target, StatusType status, DamagePacket hit) {

  /** The entity responsible for the hit that filled the meter, if any. */
  public Optional<Entity> attacker() {

    return hit.attacker();
  }
}
