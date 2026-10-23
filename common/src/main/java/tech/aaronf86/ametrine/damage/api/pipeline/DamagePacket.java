package tech.aaronf86.ametrine.damage.api.pipeline;

import it.unimi.dsi.fastutil.objects.Reference2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMaps;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

/**
 * One hit as it moves through the {@link DamagePipeline}: damage split by type, status buildup
 * (poise is a status like any other), flags and extra context data. Packets are created by
 * Ametrine's damage hook and are not thread-safe.
 */
public final class DamagePacket {

  private final LivingEntity target;
  private final DamageSource source;
  private final float vanillaAmount;
  private final Reference2FloatMap<AmetrineDamageType> damage =
      new Reference2FloatLinkedOpenHashMap<>();
  private final Reference2FloatMap<StatusType> buildup = new Reference2FloatLinkedOpenHashMap<>();
  private final Set<PacketFlag> flags = new ReferenceOpenHashSet<>();
  private final Map<PacketKey<?>, Object> context = new HashMap<>();
  private float motionValue = 1;
  private boolean cancelled;

  /**
   * @param target the entity being hit
   * @param source the vanilla damage source
   * @param vanillaAmount the damage vanilla would have dealt
   */
  public DamagePacket(LivingEntity target, DamageSource source, float vanillaAmount) {

    this.target = Objects.requireNonNull(target);
    this.source = Objects.requireNonNull(source);
    this.vanillaAmount = vanillaAmount;
  }

  /** The entity being hit. */
  public LivingEntity target() {

    return target;
  }

  /** The original vanilla damage source. */
  public DamageSource source() {

    return source;
  }

  /** The entity responsible for the hit, e.g. the player who fired an arrow. */
  public Optional<Entity> attacker() {

    return Optional.ofNullable(source.getEntity());
  }

  /** The entity that physically hit, e.g. the arrow. */
  public Optional<Entity> directEntity() {

    return Optional.ofNullable(source.getDirectEntity());
  }

  /** The damage vanilla would have dealt, before any of Ametrine's changes. */
  public float vanillaAmount() {

    return vanillaAmount;
  }

  // Damage

  /** Damage of one type, or 0. */
  public float damage(AmetrineDamageType type) {

    return damage.getFloat(type);
  }

  /** Sets damage of one type. Values of 0 or less remove the type. */
  public void setDamage(AmetrineDamageType type, float amount) {

    if (amount > 0) {
      damage.put(type, amount);
    } else {
      damage.removeFloat(type);
    }
  }

  /** Adds damage of one type. */
  public void addDamage(AmetrineDamageType type, float amount) {

    setDamage(type, damage(type) + amount);
  }

  /** Multiplies every damage type by a factor. */
  public void scaleDamage(float factor) {

    for (AmetrineDamageType type : damage.keySet().toArray(AmetrineDamageType[]::new)) {
      setDamage(type, damage.getFloat(type) * factor);
    }
  }

  /** All damage, keyed by type, in the order types were added. Read-only. */
  public Reference2FloatMap<AmetrineDamageType> damageMap() {

    return Reference2FloatMaps.unmodifiable(damage);
  }

  /** Sum of every damage type. */
  public float totalDamage() {

    float total = 0;
    for (float amount : damage.values()) {
      total += amount;
    }
    return total;
  }

  // Status buildup

  /** Buildup of one status, or 0. */
  public float buildup(StatusType status) {

    return buildup.getFloat(status);
  }

  /** Sets buildup of one status. Values of 0 or less remove the status. */
  public void setBuildup(StatusType status, float amount) {

    if (amount > 0) {
      buildup.put(status, amount);
    } else {
      buildup.removeFloat(status);
    }
  }

  /** Adds buildup of one status. */
  public void addBuildup(StatusType status, float amount) {

    setBuildup(status, buildup(status) + amount);
  }

  /** All buildup, keyed by status. Read-only. */
  public Reference2FloatMap<StatusType> buildupMap() {

    return Reference2FloatMaps.unmodifiable(buildup);
  }

  // Motion value

  /**
   * Multiplier for this particular attack, e.g. heavy attacks. Applied to damage (not buildup) at
   * the start of MITIGATE.
   */
  public float motionValue() {

    return motionValue;
  }

  /** Sets the motion value. */
  public void setMotionValue(float motionValue) {

    this.motionValue = Math.max(0, motionValue);
  }

  // Flags

  /** Whether the packet carries a flag. */
  public boolean hasFlag(PacketFlag flag) {

    return flags.contains(flag);
  }

  /** Adds a flag. */
  public void addFlag(PacketFlag flag) {

    flags.add(flag);
  }

  /** Removes a flag. */
  public void removeFlag(PacketFlag flag) {

    flags.remove(flag);
  }

  // Context

  /** Extra data stored under a key, if present. */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(PacketKey<T> key) {

    return Optional.ofNullable((T) context.get(key));
  }

  /** Stores extra data under a key. */
  public <T> void set(PacketKey<T> key, T value) {

    context.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
  }

  // Used by DamageSpec, whose context map has lost the link between each key and its value type
  void setRaw(PacketKey<?> key, Object value) {

    context.put(key, value);
  }

  // Cancellation

  /** Stops the hit. No later listener runs and no damage is dealt. */
  public void cancel() {

    cancelled = true;
  }

  /** Whether the hit was cancelled. */
  public boolean isCancelled() {

    return cancelled;
  }
}
