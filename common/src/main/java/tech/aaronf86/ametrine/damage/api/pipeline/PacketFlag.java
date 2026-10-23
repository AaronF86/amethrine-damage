package tech.aaronf86.ametrine.damage.api.pipeline;

import net.minecraft.resources.Identifier;

/** A marker on a {@link DamagePacket}. Addons can create their own. */
public record PacketFlag(Identifier id) {}
