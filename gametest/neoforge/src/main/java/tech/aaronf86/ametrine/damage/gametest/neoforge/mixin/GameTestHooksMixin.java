package tech.aaronf86.ametrine.damage.gametest.neoforge.mixin;

import net.neoforged.neoforge.gametest.GameTestHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NeoForge never ticks game tests in production, even on its own GameTest entry point.
// tools/gametest.sh runs the
// release jar on a real production server, so it passes -Dneoforge.enableGameTest=true and this
// lets that through.
// The ametrineTest$ prefix is the Mixin convention that keeps added members from clashing with
// other mods. Mixin
// classes are never instantiated, so the implicit constructor Sonar flags is harmless.
@SuppressWarnings({"java:S100", "java:S1118"})
@Mixin(GameTestHooks.class)
public abstract class GameTestHooksMixin {

  @Inject(method = "isGametestEnabled", at = @At("HEAD"), cancellable = true)
  private static void ametrineTest$enableInProduction(CallbackInfoReturnable<Boolean> cir) {

    if (Boolean.getBoolean("neoforge.enableGameTest")) {
      cir.setReturnValue(true);
    }
  }
}
