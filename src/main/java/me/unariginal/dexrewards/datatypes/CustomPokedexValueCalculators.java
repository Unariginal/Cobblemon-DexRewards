package me.unariginal.dexrewards.datatypes;

import com.cobblemon.mod.common.api.pokedex.*;
import com.cobblemon.mod.common.api.pokedex.def.PokedexDef;
import com.cobblemon.mod.common.api.pokedex.entry.DexEntries;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomPokedexValueCalculators {
    public static final class ShinyCount implements PokedexValueCalculator<Integer>, GlobalPokedexValueCalculator<Integer> {
        // 1.7
        // public boolean outputIsPercentage = false;

        @Override
        public Integer calculate(@NotNull AbstractPokedexManager dexManager) {
            AtomicInteger count = new AtomicInteger(0);
            dexManager.getSpeciesRecords().values().forEach(dexRecord -> {
                if (dexRecord.getKnowledge().equals(PokedexEntryProgress.CAUGHT)) {
                    PokedexDef dexDef = Dexes.INSTANCE.getDexEntryMap().get(Identifier.of("cobblemon", "national"));
                    List<PokedexEntry> pokedexEntries = new ArrayList<>();
                    dexDef.getEntries().forEach(dexEntry -> {
                        if (dexEntry != null) {
                            pokedexEntries.add(dexEntry);
                        }
                    });
                    for (PokedexEntry pokedexEntry : pokedexEntries) {
                        dexManager.getCaughtForms(pokedexEntry).forEach(caughtForm -> {
                            Set<String> shinyStates = dexManager.getSeenShinyStates(pokedexEntry, caughtForm);
                            boolean shiny = !shinyStates.isEmpty() && shinyStates.stream().anyMatch(state -> state.equals("shiny"));
                            if (shiny) {
                                count.getAndIncrement();
                            }
                        });
                    }
                }
            });
            return count.get();
        }

        @Override
        public Integer calculate(@NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> map) {
            AtomicInteger count = new AtomicInteger(0);
            map.values().forEach(dexEntry -> {
                if (dexManager.getKnowledgeForSpecies(dexEntry.getSpeciesId()).equals(PokedexEntryProgress.CAUGHT)) {
                    dexManager.getCaughtForms(dexEntry).forEach(caughtForm -> {
                        Set<String> shinyStates = dexManager.getSeenShinyStates(dexEntry, caughtForm);
                        boolean shiny = !shinyStates.isEmpty() && shinyStates.stream().anyMatch(state -> state.equals("shiny"));
                        if (shiny) {
                            count.getAndIncrement();
                        }
                    });
                }
            });
            return count.get();
        }
    }

    public static final class ShinyPercent implements PokedexValueCalculator<Float>, GlobalPokedexValueCalculator<Float> {
        // 1.7
        // public boolean outputIsPercentage = true;

        @Override
        public Float calculate(@NotNull AbstractPokedexManager abstractPokedexManager) {
            int shinyCount = new ShinyCount().calculate(abstractPokedexManager);
            return ((float) shinyCount / getTotalShinyEntries(Identifier.of("cobblemon", "national"))) * 100F;
        }

        @Override
        public Float calculate(@NotNull AbstractPokedexManager abstractPokedexManager, @NotNull Map<Identifier, PokedexEntry> map) {
            int shinyCount = new ShinyCount().calculate(abstractPokedexManager, map);
            return ((float) shinyCount / getTotalShinyEntries(map)) * 100F;
        }
    }

    public static int getTotalShinyEntries(Identifier dexId) {
        PokedexDef dexDef = Dexes.INSTANCE.getDexEntryMap().get(dexId);
        AtomicInteger totalEntries = new AtomicInteger(0);
        dexDef.getEntries().forEach(dexEntry -> totalEntries.addAndGet(dexEntry.getForms().size()));
        return totalEntries.get();
    }

    public static int getTotalShinyEntries(Map<Identifier, PokedexEntry> map) {
        AtomicInteger totalEntries = new AtomicInteger(0);
        map.values().forEach(pokedexEntry -> totalEntries.addAndGet(pokedexEntry.getForms().size()));
        return totalEntries.get();
    }

    /* Temporarily implementing the percentage fix from 1.7 until we update */
    public static final class SeenPercent implements PokedexValueCalculator<Float>, GlobalPokedexValueCalculator<Float> {
        @Override
        public Float calculate(@NotNull AbstractPokedexManager dexManager) {
            AtomicInteger count = new AtomicInteger(0);
            dexManager.getSpeciesRecords().values().forEach(dexRecord -> {
                if (!dexRecord.getKnowledge().equals(PokedexEntryProgress.NONE)) {
                    count.getAndIncrement();
                }
            });
            return ((float) count.get() / DexEntries.INSTANCE.getEntries().values().stream().map(PokedexEntry::getSpeciesId).toList().size()) * 100F;
        }

        @Override
        public Float calculate(@NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> map) {
            return ((float) map.values().stream().filter(pokedexEntry -> !dexManager.getKnowledgeForSpecies(pokedexEntry.getSpeciesId()).equals(PokedexEntryProgress.NONE)).toList().size() / map.size()) * 100F;
        }
    }

    public static final class CaughtPercent implements PokedexValueCalculator<Float>, GlobalPokedexValueCalculator<Float> {
        @Override
        public Float calculate(@NotNull AbstractPokedexManager dexManager) {
            AtomicInteger count = new AtomicInteger(0);
            dexManager.getSpeciesRecords().values().forEach(dexRecord -> {
                if (dexRecord.getKnowledge().equals(PokedexEntryProgress.CAUGHT)) {
                    count.getAndIncrement();
                }
            });
            return ((float) count.get() / DexEntries.INSTANCE.getEntries().values().stream().map(PokedexEntry::getSpeciesId).toList().size()) * 100F;
        }

        @Override
        public Float calculate(@NotNull AbstractPokedexManager dexManager, @NotNull Map<Identifier, PokedexEntry> map) {
            return ((float) map.values().stream().filter(pokedexEntry -> dexManager.getKnowledgeForSpecies(pokedexEntry.getSpeciesId()).equals(PokedexEntryProgress.CAUGHT)).toList().size() / map.size()) * 100F;
        }
    }
}
