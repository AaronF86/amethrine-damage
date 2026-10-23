package tech.aaronf86.ametrine.damage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.aaronf86.ametrine.damage.core.AmetrineAttributes;
import tech.aaronf86.ametrine.damage.core.DamageHook;
import tech.aaronf86.ametrine.damage.core.HitResult;
import tech.aaronf86.ametrine.damage.core.status.StatusEngine;
import tech.aaronf86.ametrine.damage.core.status.StatusHolder;
import tech.aaronf86.ametrine.damage.core.status.StatusMeters;

@SuppressWarnings({"java:S100", "java:S116"})
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements StatusHolder {

  @Unique private final StatusMeters ametrine$statusMeters = new StatusMeters();

  @Shadow
  protected abstract void hurtArmor(DamageSource damageSource, float damage);

  // Give every living entity attributes
  @WrapOperation(
      method = "<init>",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/ai/attributes/DefaultAttributes;getSupplier(Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier;"))
  private AttributeSupplier ametrine$extendAttributes(
      EntityType<? extends LivingEntity> type, Operation<AttributeSupplier> original) {

    return AmetrineAttributes.extend(original.call(type));
  }

  @Override
  public StatusMeters ametrine$statusMeters() {

    return ametrine$statusMeters;
  }

  // Decay status meters and fire any procs that are still queued
  @Inject(method = "tick", at = @At("TAIL"))
  private void ametrine$tickStatuses(CallbackInfo ci) {

    LivingEntity self = (LivingEntity) (Object) this;
    if (self.level() instanceof ServerLevel) {
      StatusEngine.tick(self);
    }
  }

  // Procs queued during the hit fire once it has finished. Player.hurtServer calls this one too.
  @Inject(method = "hurtServer", at = @At("RETURN"))
  private void ametrine$runProcs(
      ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {

    StatusEngine.runPendingProcs((LivingEntity) (Object) this);
  }

  // Replace vanilla armor reduction with the Ametrine pipeline. Player overrides actuallyHurt but
  // still calls this.
  @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
  private void ametrine$replaceArmor(
      DamageSource damageSource, float damage, CallbackInfoReturnable<Float> cir) {

    // An untyped hit comes back as HitResult.Vanilla: vanilla armor applies, durability loss
    // included
    if (DamageHook.process((LivingEntity) (Object) this, damageSource, damage)
        instanceof HitResult.Replaced(float amount)) {
      if (!damageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
        hurtArmor(damageSource, damage); // keep armor durability loss
      }
      cir.setReturnValue(amount);
    }
  }
}
