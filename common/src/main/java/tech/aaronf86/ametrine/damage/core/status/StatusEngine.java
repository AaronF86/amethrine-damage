package tech.aaronf86.ametrine.damage.core.status;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import tech.aaronf86.ametrine.damage.api.AmetrineDamageApi;
import tech.aaronf86.ametrine.damage.api.defense.DefenseProfile;
import tech.aaronf86.ametrine.damage.api.pipeline.DamagePacket;
import tech.aaronf86.ametrine.damage.api.pipeline.PacketFlags;
import tech.aaronf86.ametrine.damage.api.status.StatusProcContext;
import tech.aaronf86.ametrine.damage.api.status.StatusProcs;

public final class StatusEngine {

  private static final float SECONDS_PER_TICK = 1 / 20F;

  private StatusEngine() {}

  public static void addBuildup(DamagePacket packet) {

    LivingEntity target = packet.target();
    if (packet.buildupMap().isEmpty()
        || packet.hasFlag(PacketFlags.STATUS_PROC)
        || !(target.level() instanceof ServerLevel level)) {
      return;
    }
    StatusMeters meters = StatusHolder.of(target);
    DefenseProfile defense = AmetrineDamageApi.defenseOf(target);
    packet
        .buildupMap()
        .forEach(
            (status, amount) -> {
              if (meters.add(status, amount, defense.resistance(status))) {
                meters.queue(new StatusProcContext(level, target, status, packet));
              }
            });
  }

  public static void runPendingProcs(LivingEntity entity) {

    for (StatusProcContext proc : StatusHolder.of(entity).drain()) {
      if (!entity.isRemoved()) {
        StatusProcs.fire(proc);
      }
    }
  }

  public static void tick(LivingEntity entity) {

    StatusHolder.of(entity).decay(SECONDS_PER_TICK);
    runPendingProcs(entity);
  }
}
