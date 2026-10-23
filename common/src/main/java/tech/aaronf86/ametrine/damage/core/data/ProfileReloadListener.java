package tech.aaronf86.ametrine.damage.core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NullMarked;
import tech.aaronf86.ametrine.damage.Constants;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostic;
import tech.aaronf86.ametrine.damage.core.diagnostic.DiagnosticCode;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostics;
import tech.aaronf86.ametrine.damage.core.diagnostic.Suggestions;

// Files that fail to parse are reported by vanilla's loader, which also applies NeoForge load
// conditions
@NullMarked
public final class ProfileReloadListener<T>
    extends SimpleJsonResourceReloadListener<ProfileReloadListener.TargetedFile<T>> {

  private final String directory;
  private final String targetKind;
  private final FileToIdConverter converter;
  private final Optional<Registry<?>> targets;
  private final Consumer<Map<Identifier, T>> sink;

  /**
   * @param targetKind what a target is, for messages, e.g. "item"
   * @param targets the registry targets must exist in, if they can be checked when data loads
   */
  public ProfileReloadListener(
      String directory,
      String targetsKey,
      String targetKind,
      Optional<Registry<?>> targets,
      MapCodec<T> valueCodec,
      Consumer<Map<Identifier, T>> sink) {

    this(
        directory,
        FileToIdConverter.json(directory),
        targetsKey,
        targetKind,
        targets,
        valueCodec,
        sink);
  }

  private ProfileReloadListener(
      String directory,
      FileToIdConverter converter,
      String targetsKey,
      String targetKind,
      Optional<Registry<?>> targets,
      MapCodec<T> valueCodec,
      Consumer<Map<Identifier, T>> sink) {

    super(fileCodec(targetsKey, valueCodec), converter);
    this.directory = directory;
    this.converter = converter;
    this.targetKind = targetKind;
    this.targets = targets;
    this.sink = sink;
  }

  private static <T> Codec<TargetedFile<T>> fileCodec(String targetsKey, MapCodec<T> valueCodec) {

    return RecordCodecBuilder.create(
        instance ->
            instance
                .group(
                    Identifier.CODEC
                        .listOf()
                        .fieldOf(targetsKey)
                        .forGetter(TargetedFile<T>::targets),
                    valueCodec.forGetter(TargetedFile<T>::value))
                .apply(instance, TargetedFile::new));
  }

  private static String capitalized(String text) {

    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  @Override
  protected void apply(
      Map<Identifier, TargetedFile<T>> files,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {

    Map<Identifier, T> byTarget = new HashMap<>();
    Map<Identifier, Identifier> definedIn = new HashMap<>();
    Map<String, Integer> absentMods = new TreeMap<>();
    Set<String> loadedNamespaces =
        targets
            .map(
                registry ->
                    registry.keySet().stream()
                        .map(Identifier::getNamespace)
                        .collect(Collectors.toSet()))
            .orElse(Set.of());
    int problems = 0;

    for (Map.Entry<Identifier, TargetedFile<T>> file : new TreeMap<>(files).entrySet()) {
      for (Identifier target : file.getValue().targets()) {
        Optional<Identifier> earlier = Optional.ofNullable(definedIn.put(target, file.getKey()));
        byTarget.put(target, file.getValue().value());
        if (earlier.isPresent()) {
          problems++;
          reportDuplicate(target, earlier.get(), file.getKey(), resourceManager);
        }
        Optional<Registry<?>> unknownIn = targets.filter(registry -> !registry.containsKey(target));
        if (unknownIn.isPresent()) {
          if (loadedNamespaces.contains(target.getNamespace())) {
            problems++;
            reportUnknown(target, file.getKey(), unknownIn.get(), resourceManager);
          } else {
            absentMods.merge(target.getNamespace(), 1, Integer::sum);
          }
        }
      }
    }
    reportAbsentMods(absentMods);

    sink.accept(Map.copyOf(byTarget));
    Constants.LOG.info(
        "Loaded {} entries from {} files in {}{}",
        byTarget.size(),
        files.size(),
        directory,
        problems == 0 ? "" : " with " + problems + " problems, see above");
  }

  private void reportDuplicate(
      Identifier target, Identifier earlier, Identifier later, ResourceManager resourceManager) {

    Diagnostics.report(
        Diagnostic.of(
                DiagnosticCode.DUPLICATE_TARGET,
                capitalized(targetKind) + " " + target + " is defined in two files")
            .with("ignored", location(earlier, resourceManager))
            .with("used", location(later, resourceManager))
            .help(
                "Remove "
                    + target
                    + " from one of the files. When files overlap, the file whose id sorts last "
                    + "wins."));
  }

  private void reportUnknown(
      Identifier target, Identifier file, Registry<?> registry, ResourceManager resourceManager) {

    List<String> sameMod =
        registry.keySet().stream()
            .filter(id -> id.getNamespace().equals(target.getNamespace()))
            .map(Identifier::toString)
            .toList();
    Diagnostics.report(
        Diagnostic.of(
                DiagnosticCode.UNKNOWN_TARGET,
                "Profile targets an unknown " + targetKind + ": " + target)
            .with("file", location(file, resourceManager))
            .help(
                Suggestions.closest(target.toString(), sameMod)
                    .map(suggestion -> "Did you mean " + suggestion + "?")
                    .orElse(
                        "Check the spelling. The /give or /summon command's tab completion lists valid ids.")));
  }

  private void reportAbsentMods(Map<String, Integer> absentMods) {

    if (absentMods.isEmpty()) {
      return;
    }
    Diagnostics.report(
        Diagnostic.of(
                DiagnosticCode.ABSENT_MOD_TARGET,
                "Skipped "
                    + absentMods.values().stream().mapToInt(Integer::intValue).sum()
                    + " "
                    + directory
                    + " targets from mods that are not installed")
            .with(
                "mods",
                absentMods.entrySet().stream()
                    .map(mod -> mod.getKey() + " (" + mod.getValue() + ")")
                    .collect(Collectors.joining(", ")))
            .help("Nothing to fix if those mods are meant to be optional."));
  }

  // e.g. "data/mypack/ametrinedamage/item_profiles/swords.json (pack file/MyPack)"
  private String location(Identifier file, ResourceManager resourceManager) {

    Identifier path = converter.idToFile(file);
    return "data/"
        + path.getNamespace()
        + "/"
        + path.getPath()
        + resourceManager
            .getResource(path)
            .map(Resource::sourcePackId)
            .map(pack -> " (pack " + pack + ")")
            .orElse("");
  }

  public record TargetedFile<T>(List<Identifier> targets, T value) {}
}
