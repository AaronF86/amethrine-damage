package tech.aaronf86.ametrine.damage.api.pipeline;

import net.minecraft.resources.Identifier;

/**
 * A typed slot for extra data on a {@link DamagePacket}, such as the attack that caused a hit or
 * how charged it was. Keys are compared by id, so create each key once and share it.
 *
 * @param id unique id, in the namespace of the mod that owns the data
 * @param <T> the type of value stored under this key
 */
// T is only used at compile time, to type the values DamagePacket.get and set take for this key
@SuppressWarnings("java:S2326")
public record PacketKey<T>(Identifier id) {}
