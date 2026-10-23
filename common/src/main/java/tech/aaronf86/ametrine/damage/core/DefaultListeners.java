package tech.aaronf86.ametrine.damage.core;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import tech.aaronf86.ametrine.damage.api.AmetrineDamageApi;
import tech.aaronf86.ametrine.damage.api.attack.AttackProfile;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.defense.DefenseFormula;
import tech.aaronf86.ametrine.damage.api.defense.DefenseProfile;
import tech.aaronf86.ametrine.damage.api.defense.Negation;
import tech.aaronf86.ametrine.damage.api.pipeline.DamagePacket;
import tech.aaronf86.ametrine.damage.api.pipeline.DamagePipeline;
import tech.aaronf86.ametrine.damage.api.pipeline.DamageStage;
import tech.aaronf86.ametrine.damage.api.pipeline.PacketFlags;
import tech.aaronf86.ametrine.damage.core.data.CombatProfile;
import tech.aaronf86.ametrine.damage.core.data.Profiles;
import tech.aaronf86.ametrine.damage.core.status.StatusEngine;

public final class DefaultListeners {

  public static final Identifier BUILD = AmetrineDamageApi.id("build");
  public static final Identifier MITIGATE = AmetrineDamageApi.id("mitigate");
  public static final Identifier BUILDUP = AmetrineDamageApi.id("buildup");

  private DefaultListeners() {}

  public static void register() {

    DamagePipeline.register(DamageStage.BUILD, BUILD, DefaultListeners::build);
    DamagePipeline.register(DamageStage.MITIGATE, MITIGATE, DefaultListeners::mitigate);
    DamagePipeline.register(DamageStage.APPLY, BUILDUP, StatusEngine::addBuildup);
  }

  private static void build(DamagePacket packet) {

    if (!packet.damageMap().isEmpty() || packet.hasFlag(PacketFlags.EXPLICIT)) {
      return; // a DamageSpec already built this hit
    }
    DamageSource source = packet.source();
    Optional<AttackProfile> attack = meleeAttack(source);
    if (attack.isPresent()) {
      attack.get().applyTo(packet);
      return;
    }
    Profiles.source(source)
        .ifPresent(
            mapping -> {
              mapping.attack().applyTo(packet);
              if (mapping.bypassDefense()) {
                packet.addFlag(PacketFlags.BYPASS_DEFENSE);
              }
            });
  }

  // A direct melee hit uses the weapon's profile, falling back to the attacker's own
  private static Optional<AttackProfile> meleeAttack(DamageSource source) {

    if (!(source.getEntity() instanceof LivingEntity living)
        || living != source.getDirectEntity()) {
      return Optional.empty();
    }
    ItemStack weapon =
        Optional.ofNullable(source.getWeaponItem()).orElseGet(living::getMainHandItem);
    return Profiles.item(weapon)
        .flatMap(CombatProfile::attack)
        .or(() -> Profiles.entity(living).flatMap(CombatProfile::attack));
  }

  private static void mitigate(DamagePacket packet) {

    packet.scaleDamage(packet.motionValue());
    if (packet.hasFlag(PacketFlags.BYPASS_DEFENSE)) {
      return;
    }
    DefenseProfile defense = AmetrineDamageApi.defenseOf(packet.target());
    DefenseFormula formula = AmetrineDamageApi.defenseFormula();
    for (AmetrineDamageType type : packet.damageMap().keySet().toArray(AmetrineDamageType[]::new)) {
      float afterDefense = formula.apply(packet.damage(type), defense.defense(type));
      packet.setDamage(type, Negation.apply(afterDefense, defense.negation(type)));
    }
  }
}
