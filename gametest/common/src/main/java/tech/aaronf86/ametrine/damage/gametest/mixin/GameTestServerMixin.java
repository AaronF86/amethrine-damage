package tech.aaronf86.ametrine.damage.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Objects;
import net.minecraft.gametest.framework.GameTestServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings({"java:S100", "java:S116"})
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {

  @Unique private @Nullable String ametrineTest$lastProgress;

  @WrapOperation(
      method = "tickServer",
      at =
          @At(
              value = "INVOKE",
              target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V",
              ordinal = 0),
      require = 0)
  private void ametrineTest$logProgressOnChange(
      Logger logger, String bar, Operation<Void> original) {

    if (ametrineTest$changed(bar)) {
      original.call(logger, bar);
    }
  }

  @WrapOperation(
      method = "tickServer",
      at =
          @At(
              value = "INVOKE",
              target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V",
              ordinal = 0),
      require = 0)
  private void ametrineTest$logFormattedProgressOnChange(
      Logger logger, String format, Object bar, Operation<Void> original) {

    if (ametrineTest$changed(String.valueOf(bar))) {
      original.call(logger, format, bar);
    }
  }

  @Unique
  private boolean ametrineTest$changed(String bar) {

    if (Objects.equals(bar, ametrineTest$lastProgress)) {
      return false;
    }
    ametrineTest$lastProgress = bar;
    return true;
  }
}
