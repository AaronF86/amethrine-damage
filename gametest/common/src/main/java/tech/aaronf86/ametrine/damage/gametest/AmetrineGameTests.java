package tech.aaronf86.ametrine.damage.gametest;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tech.aaronf86.ametrine.damage.api.AmetrineDamageApi;
import tech.aaronf86.ametrine.damage.api.AmetrineRegistries;
import tech.aaronf86.ametrine.damage.api.attack.AttackProfile;
import tech.aaronf86.ametrine.damage.api.pipeline.DamageSpec;
import tech.aaronf86.ametrine.damage.api.status.StatusProcContext;
import tech.aaronf86.ametrine.damage.api.status.StatusProcHook;
import tech.aaronf86.ametrine.damage.api.status.StatusProcs;
import tech.aaronf86.ametrine.damage.core.AmetrineAttributes;
import tech.aaronf86.ametrine.damage.core.data.CombatProfile;
import tech.aaronf86.ametrine.damage.core.data.Profiles;
import tech.aaronf86.ametrine.damage.core.data.SourceMapping;

public final class AmetrineGameTests {

  public static final String NAMESPACE = "ametrinedamage_test";
  private static final float EPSILON = 0.01F;

  private static final Set<UUID> BLEED_PROCS = ConcurrentHashMap.newKeySet();

  private AmetrineGameTests() {}

  public static Identifier id(String path) {

    return Identifier.fromNamespaceAndPath(NAMESPACE, path);
  }

  public static void registerAll(BiConsumer<Identifier, Consumer<GameTestHelper>> registrar) {

    registrar.accept(id("mod_loads"), AmetrineGameTests::modLoads);
    registrar.accept(id("profiles_load"), AmetrineGameTests::profilesLoad);
    registrar.accept(id("profiled_melee_hit"), AmetrineGameTests::profiledMeleeHit);
    registrar.accept(id("source_mapping_bypass"), AmetrineGameTests::sourceMappingBypass);
    registrar.accept(id("fallback_vanilla_armor"), AmetrineGameTests::fallbackVanillaArmor);
    registrar.accept(id("fallback_bypasses_armor"), AmetrineGameTests::fallbackBypassesArmor);
    registrar.accept(id("explicit_spec"), AmetrineGameTests::explicitSpec);
    registrar.accept(id("buildup_and_decay"), AmetrineGameTests::buildupAndDecay);
    registrar.accept(id("proc_at_threshold"), AmetrineGameTests::procAtThreshold);
  }

  public static void registerHooks() {

    StatusProcs.registerHook(
        id("record_bleed_procs"),
        new StatusProcHook() {

          @Override
          public void afterProc(StatusProcContext context) {

            if (context.status() == TestTypes.BLEED) {
              BLEED_PROCS.add(context.target().getUUID());
            }
          }
        });
  }

  // The registries, attributes and mixins are all in place
  private static void modLoads(GameTestHelper helper) {

    helper.assertTrue(
        AmetrineRegistries.DAMAGE_TYPES
            .get(id("slash"))
            .filter(type -> type == TestTypes.SLASH)
            .isPresent(),
        "slash damage type is registered");
    helper.assertTrue(
        BuiltInRegistries.ATTRIBUTE.containsKey(id("defense.slash")),
        "defense.slash attribute is registered");
    helper.assertTrue(
        BuiltInRegistries.ATTRIBUTE.containsKey(id("resistance.poise")),
        "resistance.poise attribute is registered");
    Mob zombie = dummy(helper, "zombie");
    helper.assertTrue(
        AmetrineAttributes.defense(TestTypes.SLASH)
            .filter(zombie.getAttributes()::hasAttribute)
            .isPresent(),
        "living entities get Ametrine's attributes");
    helper.assertValueEqual(
        AmetrineDamageApi.buildupOf(zombie, TestTypes.BLEED), 0F, "a new entity's bleed meter");
    helper.succeed();
  }

  // Item, entity and source mapping profiles from the test data pack are loaded and found
  private static void profilesLoad(GameTestHelper helper) {

    AttackProfile sword =
        AmetrineDamageApi.attackProfileOf(new ItemStack(Items.WOODEN_SWORD))
            .orElseThrow(() -> helper.assertionException("wooden_sword has no attack profile"));
    helper.assertValueEqual(sword.split().get(TestTypes.SLASH), 1F, "wooden_sword slash split");
    helper.assertValueEqual(
        sword.buildup().get(TestTypes.BLEED), 40F, "wooden_sword bleed buildup");

    CombatProfile husk =
        Profiles.entity(dummy(helper, "husk"))
            .orElseThrow(() -> helper.assertionException("husk has no entity profile"));
    helper.assertValueEqual(husk.defense().defense(TestTypes.SLASH), 10F, "husk slash defense");

    SourceMapping cactus =
        Profiles.source(helper.getLevel().damageSources().cactus())
            .orElseThrow(() -> helper.assertionException("cactus has no source mapping"));
    helper.assertTrue(cactus.bypassDefense(), "cactus mapping bypasses defense");
    helper.succeed();
  }

  // 8 slash against 10 slash defense: default curve at ratio 0.8 (x0.2785), then 0.5 negation
  private static void profiledMeleeHit(GameTestHelper helper) {

    Mob zombie = dummy(helper, "zombie");
    zombie.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_SWORD));
    LivingEntity husk = dummy(helper, "husk");
    assertDamage(helper, husk, helper.getLevel().damageSources().mobAttack(zombie), 8, 1.1141F);
    helper.succeed();
  }

  // The cactus mapping turns the hit into fire and skips the husk's 10 fire defense
  private static void sourceMappingBypass(GameTestHelper helper) {

    LivingEntity husk = dummy(helper, "husk");
    assertDamage(helper, husk, helper.getLevel().damageSources().cactus(), 4, 4);
    helper.succeed();
  }

  // An unmapped source stays untyped and gets vanilla armor: 4 against the husk's 2 armor (x0.984)
  private static void fallbackVanillaArmor(GameTestHelper helper) {

    LivingEntity husk = dummy(helper, "husk");
    assertDamage(helper, husk, helper.getLevel().damageSources().sweetBerryBush(), 4, 3.936F);
    helper.succeed();
  }

  // An unmapped source in #bypasses_armor skips vanilla armor too
  private static void fallbackBypassesArmor(GameTestHelper helper) {

    LivingEntity husk = dummy(helper, "husk");
    assertDamage(helper, husk, helper.getLevel().damageSources().generic(), 4, 4);
    helper.succeed();
  }

  // AmetrineDamageApi.hurt with an explicit spec: 6 magic, which the husk has no defense against
  private static void explicitSpec(GameTestHelper helper) {

    LivingEntity husk = dummy(helper, "husk");
    float before = husk.getHealth();
    DamageSpec spec = DamageSpec.builder().damage(TestTypes.MAGIC, 6).build();
    helper.assertTrue(
        AmetrineDamageApi.hurt(husk, helper.getLevel().damageSources().generic(), spec),
        "vanilla accepted the hit");
    assertNear(helper, before - husk.getHealth(), 6, "health lost to an explicit 6 magic hit");
    helper.succeed();
  }

  // A wooden_sword hit adds 40 bleed and 12 poise; bleed then drains 10 per second
  private static void buildupAndDecay(GameTestHelper helper) {

    Mob zombie = dummy(helper, "zombie");
    zombie.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_SWORD));
    LivingEntity husk = dummy(helper, "husk");
    husk.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(zombie), 8);
    assertNear(
        helper,
        AmetrineDamageApi.buildupOf(husk, TestTypes.BLEED),
        40,
        "bleed right after the hit");
    assertNear(
        helper,
        AmetrineDamageApi.buildupOf(husk, TestTypes.POISE),
        12,
        "poise right after the hit");
    int hitTick = husk.tickCount;
    helper.succeedWhen(
        () -> {
          int ticked = husk.tickCount - hitTick;
          helper.assertTrue(
              ticked >= 20, "husk should tick 20 times after the hit, has ticked " + ticked);
          assertNear(
              helper,
              AmetrineDamageApi.buildupOf(husk, TestTypes.BLEED),
              40 - 10 * ticked / 20F,
              "bleed after " + ticked + " ticks");
        });
  }

  // 100 bleed fills the test bleed's default resistance of 100: the meter empties and the proc
  // fires
  private static void procAtThreshold(GameTestHelper helper) {

    LivingEntity husk = dummy(helper, "husk");
    DamageSpec spec = DamageSpec.builder().buildup(TestTypes.BLEED, 100).build();
    AmetrineDamageApi.applyBuildup(husk, helper.getLevel().damageSources().generic(), spec);
    helper.assertTrue(BLEED_PROCS.contains(husk.getUUID()), "bleed procced");
    helper.assertValueEqual(
        AmetrineDamageApi.buildupOf(husk, TestTypes.BLEED), 0F, "bleed meter after the proc");
    helper.succeed();
  }

  @SuppressWarnings("unchecked")
  private static Mob dummy(GameTestHelper helper, String id) {

    EntityType<?> type =
        BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
    Mob mob = helper.spawnWithNoFreeWill((EntityType<Mob>) type, 1, 2, 1);
    mob.setNoGravity(true);
    Objects.requireNonNull(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(1);
    return mob;
  }

  private static void assertDamage(
      GameTestHelper helper,
      LivingEntity target,
      DamageSource source,
      float amount,
      float expected) {

    ServerLevel level = helper.getLevel();
    float before = target.getHealth();
    helper.assertTrue(target.hurtServer(level, source, amount), "vanilla accepted the hit");
    assertNear(
        helper,
        before - target.getHealth(),
        expected,
        "health lost to " + amount + " " + source.getMsgId() + " damage");
  }

  private static void assertNear(GameTestHelper helper, float actual, float expected, String what) {

    helper.assertTrue(
        Math.abs(actual - expected) <= EPSILON,
        what + " should be " + expected + ", was " + actual);
  }
}
