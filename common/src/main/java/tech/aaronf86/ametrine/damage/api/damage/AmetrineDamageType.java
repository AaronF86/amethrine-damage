package tech.aaronf86.ametrine.damage.api.damage;

import net.minecraft.resources.Identifier;

/**
 * A kind of damage, such as slash or fire. Each type gets its own defense and negation attributes.
 * Not to be confused with vanilla's {@code DamageType}, which describes where damage came from.
 *
 * @param id unique id
 * @param category broad grouping
 */
public record AmetrineDamageType(Identifier id, DamageCategory category) {}
