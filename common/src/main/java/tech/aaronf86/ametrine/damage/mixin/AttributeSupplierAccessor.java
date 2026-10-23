package tech.aaronf86.ametrine.damage.mixin;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("java:S100")
@Mixin(AttributeSupplier.class)
public interface AttributeSupplierAccessor {

  @Accessor("instances")
  Map<Holder<Attribute>, AttributeInstance> ametrine$getInstances();
}
