package tech.aaronf86.ametrine.damage.api.pipeline;

import java.util.*;
import tech.aaronf86.ametrine.damage.api.attack.AttackProfile;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

/**
 * A hit described with explicit amounts rather than as a split of the vanilla amount, for mods that
 * compute their own damage (melee combat, spells). Pass it to {@code AmetrineDamageApi.hurt} or
 * {@code AmetrineDamageApi.applyBuildup}. Packets built from a spec carry {@link
 * PacketFlags#EXPLICIT}, so Ametrine's own build step leaves them alone.
 *
 * <pre>{@code
 * DamageSpec.builder()
 *         .damage(MyDamageTypes.SLASH, 1.4F)
 *         .damage(MyDamageTypes.STRIKE, 0.6F)
 *         .buildup(MyStatusTypes.POISE, 12)
 *         .context(MyKeys.ATTACK, attackId)
 *         .build();
 * }</pre>
 *
 * @param damage damage per type, before the target's defense
 * @param buildup buildup per status
 * @param motionValue multiplier applied at the start of MITIGATE, see {@link
 *     DamagePacket#motionValue()}
 * @param flags flags added to the packet
 * @param context extra data copied onto the packet
 */
public record DamageSpec(
    Map<AmetrineDamageType, Float> damage,
    Map<StatusType, Float> buildup,
    float motionValue,
    Set<PacketFlag> flags,
    Map<PacketKey<?>, Object> context) {

  public DamageSpec {

    damage = Map.copyOf(damage);
    buildup = Map.copyOf(buildup);
    flags = Set.copyOf(flags);
    context = Map.copyOf(context);
  }

  public static Builder builder() {

    return new Builder();
  }

  /** Sum of every damage type. */
  public float totalDamage() {

    float total = 0;
    for (float amount : damage.values()) {
      total += amount;
    }
    return total;
  }

  /**
   * Copies this spec onto a packet.
   *
   * @param damageScale multiplier for damage only, used when vanilla changed the amount on the way
   *     (shield, difficulty, hurt cooldown)
   */
  public void applyTo(DamagePacket packet, float damageScale) {

    damage.forEach((type, amount) -> packet.addDamage(type, amount * damageScale));
    buildup.forEach(packet::addBuildup);
    packet.setMotionValue(motionValue);
    flags.forEach(packet::addFlag);
    packet.addFlag(PacketFlags.EXPLICIT);
    context.forEach(packet::setRaw);
  }

  public static final class Builder {

    private final Map<AmetrineDamageType, Float> damage = new LinkedHashMap<>();
    private final Map<StatusType, Float> buildup = new LinkedHashMap<>();
    private final Set<PacketFlag> flags = new HashSet<>();
    private final Map<PacketKey<?>, Object> context = new HashMap<>();
    private float motionValue = 1;

    private Builder() {}

    private static float checked(float amount) {

      if (!Float.isFinite(amount) || amount < 0) {
        throw new IllegalArgumentException("Amount must be finite and not negative, got " + amount);
      }
      return amount;
    }

    /** Adds damage of one type. */
    public Builder damage(AmetrineDamageType type, float amount) {

      damage.merge(Objects.requireNonNull(type), checked(amount), Float::sum);
      return this;
    }

    /** Adds buildup of one status. */
    public Builder buildup(StatusType status, float amount) {

      buildup.merge(Objects.requireNonNull(status), checked(amount), Float::sum);
      return this;
    }

    /** Adds an attack profile's damage as a split of {@code baseAmount}, plus its buildup. */
    public Builder profile(AttackProfile profile, float baseAmount) {

      checked(baseAmount);
      profile.split().forEach((type, fraction) -> damage(type, baseAmount * fraction));
      profile.buildup().forEach(this::buildup);
      return this;
    }

    public Builder motionValue(float motionValue) {

      this.motionValue = checked(motionValue);
      return this;
    }

    public Builder flag(PacketFlag flag) {

      flags.add(Objects.requireNonNull(flag));
      return this;
    }

    public <T> Builder context(PacketKey<T> key, T value) {

      context.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
      return this;
    }

    public DamageSpec build() {

      return new DamageSpec(damage, buildup, motionValue, flags, context);
    }
  }
}
