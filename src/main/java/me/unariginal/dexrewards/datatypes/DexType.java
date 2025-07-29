package me.unariginal.dexrewards.datatypes;

import me.unariginal.dexrewards.datatypes.rewards.RewardGroup;

import java.util.List;

public class DexType {
    public String name;
    public String displayName;
    public String pokedex;
    public boolean countSeen;
    public boolean seenOnly;
    public boolean countShiny;
    public boolean shinyOnly;
    public List<RewardGroup> rewardGroups;

    public DexType(String name, String displayName, String pokedex, boolean countSeen, boolean seenOnly, boolean countShiny, boolean shinyOnly, List<RewardGroup> rewardGroups) {
        this.name = name;
        this.displayName = displayName;
        this.pokedex = pokedex;
        this.countSeen = countSeen;
        this.seenOnly = seenOnly;
        this.countShiny = countShiny;
        this.shinyOnly = shinyOnly;
        this.rewardGroups = rewardGroups;
    }
}
