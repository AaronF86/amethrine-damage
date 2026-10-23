package tech.aaronf86.ametrine.damage.api.status;

import net.minecraft.resources.Identifier;

/**
 * A status that builds up over several hits, such as bleed or poise. Ametrine holds each entity's
 * meter; when the buildup reaches the entity's resistance to this status, the meter resets and the
 * proc action runs through {@link StatusProcs}. Its resistance attribute is {@code
 * <namespace>:resistance.<path>}.
 *
 * @param id unique id
 * @param defaultResistance base value of the resistance attribute, i.e. how much buildup fills the
 *     meter
 * @param decayPerSecond how much buildup drains each second
 * @param proc what happens when the meter fills, or {@link StatusProc#NONE} to leave it for another
 *     mod to {@link StatusProcs#bind bind}
 */
public record StatusType(
    Identifier id, float defaultResistance, float decayPerSecond, StatusProc proc) {}
