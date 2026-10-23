package tech.aaronf86.ametrine.damage.core.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiagnosticTest {

  @Test
  void rendersAlignedDetailsHelpAndCause() {

    String rendered =
        Diagnostic.of(
                DiagnosticCode.UNKNOWN_TARGET,
                "Profile targets an unknown item: minecraft:wooden_swrod")
            .with("file", "data/mypack/ametrinedamage/item_profiles/swords.json")
            .with("pack id", "file/MyPack")
            .help("Did you mean minecraft:wooden_sword?")
            .cause(new IllegalStateException("boom"))
            .render();
    assertEquals(
        """
        [AD1001] Profile targets an unknown item: minecraft:wooden_swrod
           | file:    data/mypack/ametrinedamage/item_profiles/swords.json
           | pack id: file/MyPack
           = help: Did you mean minecraft:wooden_sword?
           = caused by: java.lang.IllegalStateException: boom""",
        rendered);
  }

  @Test
  void rendersJustTheHeadlineWhenBare() {

    assertEquals(
        "[AD3006] oops",
        Diagnostic.of(DiagnosticCode.ATTRIBUTES_REGISTERED_TWICE, "oops").render());
  }

  @Test
  void suggestsCloseIdsOnly() {

    List<String> items =
        List.of("minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:wooden_axe");
    assertEquals(
        Optional.of("minecraft:wooden_sword"),
        Suggestions.closest("minecraft:wooden_swrod", items));
    assertEquals(
        Optional.of("minecraft:stone_sword"), Suggestions.closest("minecraft:stone_sord", items));
    assertEquals(Optional.empty(), Suggestions.closest("minecraft:diamond_pickaxe", items));
  }

  @Test
  void measuresEditDistance() {

    assertEquals(0, Suggestions.distance("abc", "abc"));
    assertEquals(2, Suggestions.distance("swrod", "sword"));
    assertEquals(3, Suggestions.distance("", "abc"));
  }
}
