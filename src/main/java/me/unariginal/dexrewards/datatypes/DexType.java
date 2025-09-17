package me.unariginal.dexrewards.datatypes;

import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;

import java.util.List;

public class DexType {
    public String name;
    public String displayName;
    public String pokedex;
    public boolean requireCaught;
    public boolean requireShiny;
    public List<String> validSpecies;
    public List<String> ignoredSpecies;
    public List<String> validLabels;
    public List<String> ignoredLabels;
    public List<RewardGroup> rewardGroups;

    public DexType(String name, String displayName, String pokedex, boolean requireCaught, boolean requireShiny, List<String> validSpecies, List<String> ignoredSpecies, List<String> validLabels, List<String> ignoredLabels, List<RewardGroup> rewardGroups) {
        this.name = name;
        this.displayName = displayName;
        this.pokedex = pokedex;
        this.requireCaught = requireCaught;
        this.requireShiny = requireShiny;
        this.validSpecies = validSpecies;
        this.ignoredSpecies = ignoredSpecies;
        this.validLabels = validLabels;
        this.ignoredLabels = ignoredLabels;
        this.rewardGroups = rewardGroups;
    }
}
