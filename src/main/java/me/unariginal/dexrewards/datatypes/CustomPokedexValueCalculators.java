package me.unariginal.dexrewards.datatypes;


import com.cobblemon.mod.common.api.pokedex.*;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import me.unariginal.dexrewards.DexRewards;
import me.unariginal.dexrewards.config.Config;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomPokedexValueCalculators {
    // To filter out unimplemented, invalid, or specific labels, we rewrite these calculator methods
    // Also to include shiny count calculation

    /**
     * Calculates the amount of caught Pokémon (globally or per dex)
     */
    public static class CaughtCount {
        public Integer calculate(@NotNull DexType dexType, @NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> dex) {
            return dex.values().stream()
                    .filter(pokedexEntry -> isValidSpecies(dexType, pokedexEntry.getSpeciesId()))
                    .filter(pokedexEntry ->
                            dexManager.getKnowledgeForSpecies(pokedexEntry.getSpeciesId()) == PokedexEntryProgress.CAUGHT
                    ).toList().size();
        }
    }

    /**
     * Calculates the amount of seen Pokémon (globally or per dex)
     */
    public static class SeenCount {
        public Integer calculate(@NotNull DexType dexType, @NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> dex) {
            return dex.values().stream()
                    .filter(pokedexEntry -> isValidSpecies(dexType, pokedexEntry.getSpeciesId()))
                    .filter(pokedexEntry ->
                            dexManager.getKnowledgeForSpecies(pokedexEntry.getSpeciesId()) != PokedexEntryProgress.NONE
                    ).toList().size();
        }
    }

    /**
     * Calculates the amount of caught shiny Pokémon (globally or per dex)
     * I'm guessing this is going to be a bit weird because shiny states are by form rather than by species...
     * So it'll be like, needing to catch 2000 instead of 1025
     * I believe I counter this by only requiring one of the forms to have a "seenShinyState"
     * <p>
     * This method is bad, you can catch a non-shiny and scan a shiny, and it still counts.
     */
    public static class CaughtShinyCount {
        public Integer calculate(@NotNull DexType dexType, @NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> dex) {
            return dex.values().stream()
                    .filter(pokedexEntry -> isValidSpecies(dexType, pokedexEntry.getSpeciesId()))
                    .filter(pokedexEntry ->
                            dexManager.getKnowledgeForSpecies(pokedexEntry.getSpeciesId()) == PokedexEntryProgress.CAUGHT && hasSeenShiny(dexManager, pokedexEntry)
                    ).toList().size();
        }
    }

    /**
     * Calculates the amount of seen shiny Pokémon (globally or per dex)
     */
    public static class SeenShinyCount {
       public Integer calculate(@NotNull DexType dexType, @NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> dex) {
            return dex.values().stream()
                    .filter(pokedexEntry -> isValidSpecies(dexType, pokedexEntry.getSpeciesId()))
                    .filter(pokedexEntry ->
                            dexManager.getKnowledgeForSpecies(pokedexEntry.getSpeciesId()) != PokedexEntryProgress.NONE && hasSeenShiny(dexManager, pokedexEntry)
                    ).toList().size();
        }
    }

    /**
     * Calculates the caught percentage globally or relative to a particular dex
     */
    public static class CaughtPercent {
        public Float calculate(@NotNull DexType dexType, @NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> dex) {
            return new CaughtCount().calculate(dexType, dexManager, dex).floatValue() / DexRewards.INSTANCE.dexTypeTotals.get(dexType) * 100F;
        }
    }

    /**
     * Calculates the seen percentage globally or relative to a particular dex
     */
    public static class SeenPercent {
        public Float calculate(@NotNull DexType dexType, @NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> dex) {
            return new SeenCount().calculate(dexType, dexManager, dex).floatValue() / DexRewards.INSTANCE.dexTypeTotals.get(dexType) * 100F;
        }
    }

    /**
     * Calculates the caught shiny percentage globally or relative to a particular dex
     */
    public static class CaughtShinyPercent {
        public Float calculate(@NotNull DexType dexType, @NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> dex) {
            return new CaughtShinyCount().calculate(dexType, dexManager, dex).floatValue() / DexRewards.INSTANCE.dexTypeTotals.get(dexType) * 100F;
        }
    }

    /**
     * Calculates the seen shiny percentage globally or relative to a particular dex
     */
    public static class SeenShinyPercent {
        public Float calculate(@NotNull DexType dexType, @NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> dex) {
            return new SeenShinyCount().calculate(dexType, dexManager, dex).floatValue() / DexRewards.INSTANCE.dexTypeTotals.get(dexType) * 100F;
        }
    }

    public static int getDexSize(@NotNull DexType dexType) {
        return Dexes.INSTANCE.getDexEntryMap().get(Identifier.of(dexType.pokedex))
                .getEntries().stream()
                .map(PokedexEntry::getSpeciesId)
                .filter(speciesId -> isValidSpecies(dexType, speciesId))
                .toList().size();
    }

    public static boolean hasSeenShiny(@NotNull AbstractPokedexManager dexManager, @NotNull PokedexEntry pokedexEntry) {
        AtomicBoolean hasSeenShinyState = new AtomicBoolean(false);
        pokedexEntry.getForms().forEach(form -> {
            SpeciesDexRecord speciesDexRecord = dexManager.getSpeciesRecord(pokedexEntry.getSpeciesId());
            if (speciesDexRecord != null) {
                FormDexRecord formDexRecord = speciesDexRecord.getFormRecord(form.getDisplayForm());
                if (formDexRecord != null && formDexRecord.hasSeenShinyState(true))
                    hasSeenShinyState.set(true);
            }
        });

        return hasSeenShinyState.get();
    }

    public static boolean isValidSpecies(@NotNull DexType dexType, @NotNull Identifier speciesId) {
        Species species = PokemonSpecies.INSTANCE.getByIdentifier(speciesId);
        if (species != null) {
            if (!Config.configData.implementedOnly || species.getImplemented()) {
                if (!dexType.validSpecies.isEmpty() && !dexType.validSpecies.contains(species.showdownId().toLowerCase())) {
                    return false;
                }
                if (dexType.ignoredSpecies.contains(species.showdownId().toLowerCase())) {
                    return false;
                }
                if (!dexType.validLabels.isEmpty()) {
                    boolean match = false;
                    for (String label : dexType.validLabels) {
                        if (species.getLabels().contains(label)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        return false;
                    }
                }

                for (String label : dexType.ignoredLabels) {
                    if (species.getLabels().contains(label)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } else {
            return Config.configData.allowInvalidSpecies;
        }

        return true;
    }
}
