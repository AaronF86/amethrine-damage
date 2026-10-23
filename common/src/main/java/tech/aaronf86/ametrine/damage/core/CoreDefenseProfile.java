package tech.aaronf86.ametrine.damage.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import tech.aaronf86.ametrine.damage.api.damage.AmetrineDamageType;
import tech.aaronf86.ametrine.damage.api.defense.DefenseProfile;
import tech.aaronf86.ametrine.damage.api.defense.DefenseValues;
import tech.aaronf86.ametrine.damage.api.defense.Negation;
import tech.aaronf86.ametrine.damage.api.status.StatusType;
import tech.aaronf86.ametrine.damage.core.data.CombatProfile;
import tech.aaronf86.ametrine.damage.core.data.Profiles;

public final class CoreDefenseProfile implements DefenseProfile {

  private final LivingEntity entity;
  private final List<DefenseValues> sources = new ArrayList<>();

  public CoreDefenseProfile(LivingEntity entity) {

    this.entity = entity;
    Profiles.entity(entity).map(CombatProfile::defense).ifPresent(sources::add);
    for (EquipmentSlot slot : EquipmentSlot.VALUES) {
      if (slot.isArmor()) {
        Profiles.item(entity.getItemBySlot(slot))
            .map(CombatProfile::defense)
            .ifPresent(sources::add);
      }
    }
  }

  @Override
  public float defense(AmetrineDamageType type) {

    float total = AmetrineAttributes.defense(type).flatMap(this::attribute).orElse(0F);
    for (DefenseValues source : sources) {
      total += source.defense(type);
    }
    return total;
  }

  @Override
  public float negation(AmetrineDamageType type) {

    float taken =
        1 - Negation.clamp(AmetrineAttributes.negation(type).flatMap(this::attribute).orElse(0F));
    for (DefenseValues source : sources) {
      taken *= 1 - Negation.clamp(source.negation(type));
    }
    return 1 - taken;
  }

  @Override
  public float resistance(StatusType status) {

    float total =
        AmetrineAttributes.resistance(status)
            .flatMap(this::attribute)
            .orElse(status.defaultResistance());
    for (DefenseValues source : sources) {
      total += source.resistance(status);
    }
    return total;
  }

  // The attribute's value, if it exists and this entity has it
  private Optional<Float> attribute(Holder<Attribute> attribute) {

    return entity.getAttributes().hasAttribute(attribute)
        ? Optional.of((float) entity.getAttributeValue(attribute))
        : Optional.empty();
  }
}
