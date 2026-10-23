package tech.aaronf86.ametrine.damage.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.*;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostic;
import tech.aaronf86.ametrine.damage.core.diagnostic.DiagnosticCode;
import tech.aaronf86.ametrine.damage.core.diagnostic.Diagnostics;

/**
 * A simple id-keyed registry for Ametrine content. Entries can be added until the registry is
 * frozen, which happens during game startup once Ametrine creates the attributes for every entry.
 *
 * @param <T> the entry type
 */
public final class AmetrineRegistry<T> {

  private final Identifier name;
  private final Function<T, Identifier> idGetter;
  private final Map<Identifier, T> entries = new LinkedHashMap<>();
  private final Collection<T> view = Collections.unmodifiableCollection(entries.values());
  private final Codec<T> codec;
  private boolean frozen;

  /**
   * @param name the registry's own id, used in error messages
   * @param idGetter returns the id of an entry
   */
  public AmetrineRegistry(Identifier name, Function<T, Identifier> idGetter) {

    this.name = name;
    this.idGetter = idGetter;
    this.codec = Identifier.CODEC.comapFlatMap(this::lookup, idGetter);
  }

  /**
   * Registers an entry.
   *
   * @throws IllegalStateException if the registry is frozen
   * @throws IllegalArgumentException if the id is already taken
   */
  public <E extends T> E register(E entry) {

    Identifier id = idGetter.apply(entry);
    if (frozen) {
      throw Diagnostics.failure(
          Diagnostic.of(
                  DiagnosticCode.REGISTRY_FROZEN,
                  "Tried to register " + id + " after registry " + name + " was frozen")
              .with("mod", id.getNamespace())
              .with("registry", name)
              .help(
                  "Register from an AmetrineAddon entrypoint on Fabric, or from your mod constructor on NeoForge. "
                      + "The registry freezes when Ametrine creates its attributes at startup."));
    }
    if (entries.putIfAbsent(id, entry) != null) {
      throw Diagnostics.badArgument(
          Diagnostic.of(DiagnosticCode.DUPLICATE_ID, id + " is already registered in " + name)
              .with("mod", id.getNamespace())
              .with("registry", name)
              .help(
                  "Each id can be registered once. Check that your registration code doesn't run twice, and "
                      + "that ids use your own mod's namespace."));
    }
    return entry;
  }

  /** Returns the entry with the given id, if present. */
  public Optional<T> get(Identifier id) {

    return Optional.ofNullable(entries.get(id));
  }

  /** Returns the id of an entry. */
  public Identifier getId(T entry) {

    return idGetter.apply(entry);
  }

  /** Returns every entry in registration order. */
  public Collection<T> values() {

    return view;
  }

  /** Returns a codec that reads and writes entries by id. */
  public Codec<T> codec() {

    return codec;
  }

  /** Returns this registry's own id. */
  public Identifier name() {

    return name;
  }

  /** Returns true once no more entries can be registered. */
  public boolean isFrozen() {

    return frozen;
  }

  /** Stops further registration. Called by Ametrine during startup. */
  public void freeze() {

    frozen = true;
  }

  private DataResult<T> lookup(Identifier id) {

    return get(id)
        .map(DataResult::success)
        .orElseGet(() -> DataResult.error(() -> "Unknown entry " + id + " in registry " + name));
  }
}
