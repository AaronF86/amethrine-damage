package tech.aaronf86.ametrine.damage.core;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import tech.aaronf86.ametrine.damage.Constants;
import tech.aaronf86.ametrine.damage.api.pipeline.*;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostic;
import tech.aaronf86.ametrine.damage.core.diagnostic.DiagnosticCode;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostics;
import tech.aaronf86.ametrine.damage.core.status.StatusEngine;

public final class DamageHook {

  private static final boolean DEBUG = Boolean.getBoolean("ametrinedamage.debug");
  private static final ThreadLocal<@Nullable PendingSpec> PENDING = new ThreadLocal<>();

  private DamageHook() {}

  private static String describe(DamagePacket packet) {

    StringBuilder out = new StringBuilder(packet.damageMap().isEmpty() ? "untyped " : "");
    packet
        .damageMap()
        .forEach(
            (type, amount) ->
                out.append(type.id().getPath()).append('=').append(amount).append(' '));
    if (!packet.buildupMap().isEmpty()) {
      out.append("| ");
      packet
          .buildupMap()
          .forEach(
              (status, amount) ->
                  out.append(status.id().getPath()).append('+').append(amount).append(' '));
    }
    return out.toString().trim();
  }

  public static HitResult process(LivingEntity target, DamageSource source, float amount) {

    if (amount <= 0) {
      return new HitResult.Replaced(amount);
    }
    DamagePacket packet = new DamagePacket(target, source, amount);
    Optional.ofNullable(PENDING.get())
        .filter(pending -> pending.target() == target && pending.source() == source)
        .ifPresent(
            pending -> {
              PENDING.remove();
              float total = pending.spec().totalDamage();
              pending.spec().applyTo(packet, total > 0 ? amount / total : 1);
            });
    try {
      DamagePipeline.process(packet);
    } catch (DamageListenerException e) {
      reportListenerFailure(
          e,
          packet,
          "the hit dealt its incoming damage, "
              + amount
              + ", with no Ametrine "
              + "defense applied");
      return new HitResult.Replaced(amount);
    }
    if (!packet.isCancelled()
        && packet.damageMap().isEmpty()
        && !packet.hasFlag(PacketFlags.EXPLICIT)) {
      log(packet, amount, amount);
      return HitResult.VANILLA;
    }
    float result = packet.isCancelled() ? 0 : packet.totalDamage();
    log(packet, amount, result);
    return new HitResult.Replaced(result);
  }

  public static boolean hurt(LivingEntity target, DamageSource source, DamageSpec spec) {

    if (!(target.level() instanceof ServerLevel level)) {
      return false;
    }
    float total = spec.totalDamage();
    if (total <= 0) {
      applyBuildup(target, source, spec);
      return false;
    }
    PendingSpec previous = PENDING.get();
    PENDING.set(new PendingSpec(target, source, spec));
    try {
      return target.hurtServer(level, source, total);
    } finally {
      PENDING.set(previous);
    }
  }

  public static void applyBuildup(LivingEntity target, DamageSource source, DamageSpec spec) {

    if (!(target.level() instanceof ServerLevel)) {
      return;
    }
    DamagePacket packet = new DamagePacket(target, source, 0);
    spec.applyTo(packet, 0);
    try {
      DamagePipeline.process(packet);
    } catch (DamageListenerException e) {
      reportListenerFailure(e, packet, "the buildup was dropped");
      return;
    }
    log(packet, 0, 0);
    StatusEngine.runPendingProcs(target);
  }

  private static void reportListenerFailure(
      DamageListenerException e, DamagePacket packet, String fallback) {

    Identifier listener = e.listener();
    Diagnostics.reportRepeating(
        DiagnosticCode.LISTENER_FAILED.id() + " " + listener,
        () ->
            Diagnostic.of(
                    DiagnosticCode.LISTENER_FAILED,
                    "Damage listener " + listener + " threw in stage " + e.stage())
                .with("mod", listener.getNamespace())
                .with("target", packet.target().getType().getDescriptionId())
                .with("source", packet.source().getMsgId())
                .with(
                    "attacker",
                    packet
                        .attacker()
                        .map(attacker -> attacker.getType().getDescriptionId())
                        .orElse("none"))
                .with("packet", describe(packet))
                .with("fallback", fallback)
                .help(
                    "This is a bug in "
                        + listener.getNamespace()
                        + ". Report it to that mod with this log. Hits that "
                        + "reach this listener fall back until it is fixed. Start with -Dametrinedamage.debug=true to trace each hit.")
                .cause(e.getCause()));
  }

  private static void log(DamagePacket packet, float vanilla, float result) {

    if (DEBUG && Constants.LOG.isInfoEnabled()) {
      DamageSource source = packet.source();
      Constants.LOG.info(
          "[debug] {} hit by {} ({}): vanilla {} -> {} {}{}",
          packet.target().getType().getDescriptionId(),
          source.getMsgId(),
          packet
              .attacker()
              .map(attacker -> attacker.getType().getDescriptionId())
              .orElse("no attacker"),
          vanilla,
          result,
          describe(packet),
          packet.isCancelled() ? " [cancelled]" : "");
    }
  }

  private record PendingSpec(LivingEntity target, DamageSource source, DamageSpec spec) {}
}
