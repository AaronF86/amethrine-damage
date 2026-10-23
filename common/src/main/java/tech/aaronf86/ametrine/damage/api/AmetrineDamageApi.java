package tech.aaronf86.ametrine.damage.api;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import tech.aaronf86.ametrine.damage.api.attack.AttackProfile;
import tech.aaronf86.ametrine.damage.api.defense.DefenseFormula;
import tech.aaronf86.ametrine.damage.api.defense.DefenseFormulas;
import tech.aaronf86.ametrine.damage.api.defense.DefenseProfile;
import tech.aaronf86.ametrine.damage.api.pipeline.DamageSpec;
import tech.aaronf86.ametrine.damage.api.status.StatusType;
import tech.aaronf86.ametrine.damage.core.DamageHook;
import tech.aaronf86.ametrine.damage.core.data.CombatProfile;
import tech.aaronf86.ametrine.damage.core.data.Profiles;
import tech.aaronf86.ametrine.damage.core.status.StatusHolder;

/** Global entry points and swappable pieces of the damage system. */
public final class AmetrineDamageApi {

  /** Namespace used for Ametrine's own ids. */
  public static final String NAMESPACE = "ametrinedamage";

  private static DefenseFormula defenseFormula = DefenseFormulas.DEFAULT_CURVE;
  private static Function<LivingEntity, DefenseProfile> defenseProvider =
      entity -> DefenseProfile.NONE;

  private AmetrineDamageApi() {}

  /** Creates an id in Ametrine's namespace. */
  public static Identifier id(String path) {

    return Identifier.fromNamespaceAndPath(NAMESPACE, path);
  }

  /** The formula used to reduce each damage type by flat defense. */
  public static DefenseFormula defenseFormula() {

    return defenseFormula;
  }

  /** Replaces the defense formula, for modpacks that want a different curve. */
  public static void setDefenseFormula(DefenseFormula formula) {

    defenseFormula = Objects.requireNonNull(formula);
  }

  /** Returns the defensive stats of an entity. */
  public static DefenseProfile defenseOf(LivingEntity entity) {

    return defenseProvider.apply(entity);
  }

  /**
   * Replaces how defensive stats are read. Ametrine installs an attribute-based provider at
   * startup.
   */
  public static void setDefenseProvider(Function<LivingEntity, DefenseProfile> provider) {

    defenseProvider = Objects.requireNonNull(provider);
  }

  /**
   * Hits an entity with explicit damage, buildup and context, going through vanilla's hurt logic
   * (hurt cooldown, shields, knockback, death) and then Ametrine's pipeline. If vanilla reduces the
   * amount on the way, the spec's damage is scaled by the same factor. A spec with no damage is
   * passed to {@link #applyBuildup} instead. Server side only.
   *
   * @return whether vanilla accepted the hit
   */
  public static boolean hurt(LivingEntity target, DamageSource source, DamageSpec spec) {

    return DamageHook.hurt(target, source, spec);
  }

  /**
   * Runs a spec through the pipeline without vanilla's hurt logic: buildup (e.g. poise on a blocked
   * hit) is added and procs fire, but no health is lost and there is no hurt animation or
   * knockback. Server side only.
   */
  public static void applyBuildup(LivingEntity target, DamageSource source, DamageSpec spec) {

    DamageHook.applyBuildup(target, source, spec);
  }

  /** The attack profile data packs give an item when used as a melee weapon. */
  public static Optional<AttackProfile> attackProfileOf(ItemStack stack) {

    return Profiles.item(stack).flatMap(CombatProfile::attack);
  }

  /** The attack profile data packs give an entity type when it hits without a profiled weapon. */
  public static Optional<AttackProfile> attackProfileOf(Entity entity) {

    return Profiles.entity(entity).flatMap(CombatProfile::attack);
  }

  /**
   * How full an entity's meter for a status is. Meters are server side and start empty after a
   * reload.
   */
  public static float buildupOf(LivingEntity entity, StatusType status) {

    return StatusHolder.of(entity).get(status);
  }

  /**
   * Sets an entity's meter for a status directly, e.g. to empty the poise meter when a stagger
   * ends. Does not proc.
   */
  public static void setBuildup(LivingEntity entity, StatusType status, float amount) {

    StatusHolder.of(entity).set(status, amount);
  }
}
