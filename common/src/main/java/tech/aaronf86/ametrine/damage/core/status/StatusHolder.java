package tech.aaronf86.ametrine.damage.core.status;

import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings("java:S100")
public interface StatusHolder {

  static StatusMeters of(LivingEntity entity) {

    return ((StatusHolder) entity).ametrine$statusMeters();
  }

  StatusMeters ametrine$statusMeters();
}
